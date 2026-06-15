package com.jsantos.imdbgraphql.imdb.scraper;

import com.jsantos.imdbgraphql.config.ImdbProperties;
import com.jsantos.imdbgraphql.imdb.dto.ImdbEstreiaDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbFilmeDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbPessoaDTO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Acesso aos dados do IMDB pela <strong>API GraphQL pública</strong>
 * ({@code https://api.graphql.imdb.com/}) — sem Playwright/Chromium.
 *
 * <p>As páginas HTML ({@code www.imdb.com}) estão atrás de um desafio JavaScript
 * do AWS WAF e exigiriam um navegador real; já o endpoint GraphQL aceita queries
 * arbitrárias via POST, devolve JSON puro e não passa pelo WAF. A localização do
 * título/sinopse é feita pelos cabeçalhos {@code x-imdb-user-language} /
 * {@code x-imdb-user-country} derivados do {@code locale}. O mapeamento do JSON
 * para os DTOs é delegado ao {@link ImdbGraphqlMapper}.
 */
@Component
public class ImdbScraper {

    private static final URI ENDPOINT = URI.create("https://api.graphql.imdb.com/");

    private static final String QUERY_FILME = """
            query Filme($id: ID!) {
              title(id: $id) {
                titleText { text }
                originalTitleText { text }
                plot { plotText { plainText } }
                releaseYear { year }
                releaseDate { day month year }
                runtime { seconds }
                primaryImage { url }
                genres { genres { id text } }
                bilheteriaUS: lifetimeGross(boxOfficeArea: DOMESTIC) { total { amount currency } }
                bilheteriaMundo: lifetimeGross(boxOfficeArea: WORLDWIDE) { total { amount currency } }
                diretores: credits(first: 50, filter: {categories: ["director"]}) {
                  edges { node { name { id nameText { text } } } }
                }
                escritores: credits(first: 50, filter: {categories: ["writer"]}) {
                  edges { node { name { id nameText { text } } } }
                }
                produtores: credits(first: 50, filter: {categories: ["producer"]}) {
                  edges { node { name { id nameText { text } } } }
                }
                elenco: credits(first: 200, filter: {categories: ["cast"]}) {
                  edges { node { name { id nameText { text } } ... on Cast { characters { name } } } }
                }
              }
            }""";

    private static final String QUERY_ESTREIAS = """
            query Estreias($from: Date!) {
              comingSoon(first: 100, comingSoonType: MOVIE, releasingOnOrAfter: $from) {
                edges { node {
                  id
                  titleText { text }
                  titleType { text }
                  releaseYear { year }
                  releaseDate { day month year }
                  primaryImage { url }
                  genres { genres { text } }
                  principalCredits { credits { name { id nameText { text } } } }
                } }
              }
            }""";

    private static final String QUERY_PESSOA = """
            query Pessoa($id: ID!) {
              name(id: $id) {
                nameText { text }
                primaryImage { url }
                primaryProfessions { category { text } }
                bio { text { plainText } }
                birthDate { date }
                deathDate { date }
              }
            }""";

    private final HttpClient http;
    private final ObjectMapper objectMapper;
    private final ImdbGraphqlMapper mapper;
    private final ImdbProperties properties;

    public ImdbScraper(ObjectMapper objectMapper, ImdbGraphqlMapper mapper, ImdbProperties properties) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build();
    }

    public ImdbFilmeDTO scrape(String codigo, String locale) {
        JsonNode data = executar(QUERY_FILME, Map.of("id", codigo), locale, "título " + codigo);
        return mapper.mapFilme(codigo, data.path("title"));
    }

    public List<ImdbEstreiaDTO> scrapeEstreias(String locale) {
        JsonNode data = executar(QUERY_ESTREIAS, Map.of("from", LocalDate.now().toString()),
                locale, "calendário de estreias");
        return mapper.mapEstreias(data.path("comingSoon"));
    }

    public ImdbPessoaDTO scrapePessoa(String nm, String locale) {
        JsonNode data = executar(QUERY_PESSOA, Map.of("id", nm), locale, "pessoa " + nm);
        return mapper.mapPessoa(nm, data.path("name"));
    }

    private JsonNode executar(String query, Map<String, Object> variables, String locale, String descricao) {
        String payload = objectMapper.writeValueAsString(Map.of("query", query, "variables", variables));

        HttpRequest.Builder req = HttpRequest.newBuilder(ENDPOINT)
                .timeout(properties.getTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        aplicarLocale(req, locale);

        HttpResponse<String> resp;
        try {
            resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ImdbScrapeException("Falha de rede ao consultar a API GraphQL do IMDB (" + descricao + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImdbScrapeException("Consulta à API GraphQL do IMDB interrompida (" + descricao + ")", e);
        }
        if (resp.statusCode() != 200) {
            throw new ImdbScrapeException(
                    "API GraphQL do IMDB respondeu HTTP " + resp.statusCode() + " (" + descricao + ")");
        }

        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new ImdbScrapeException("API GraphQL do IMDB retornou erro (" + descricao + "): "
                    + text(errors.get(0).path("message")));
        }
        return root.path("data");
    }

    private static void aplicarLocale(HttpRequest.Builder req, String locale) {
        if (locale == null || locale.isBlank()) {
            return;
        }
        req.header("x-imdb-user-language", locale);
        int hifen = locale.indexOf('-');
        if (hifen > 0 && hifen + 1 < locale.length()) {
            req.header("x-imdb-user-country", locale.substring(hifen + 1).toUpperCase());
        }
    }

    private static String text(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }
}
