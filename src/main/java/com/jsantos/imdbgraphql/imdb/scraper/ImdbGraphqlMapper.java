package com.jsantos.imdbgraphql.imdb.scraper;

import com.jsantos.imdbgraphql.imdb.dto.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Mapeamento puro (sem rede, testável offline) das respostas da API GraphQL do
 * IMDB ({@code api.graphql.imdb.com}) para os DTOs do wrapper. Recebe os nós já
 * extraídos da resposta — {@code data.title}, {@code data.comingSoon} e
 * {@code data.name} — entregues pelo {@link ImdbScraper}.
 */
@Component
public class ImdbGraphqlMapper {

    // ===================== FILME =====================

    /** @param title nó {@code data.title} da resposta GraphQL */
    public ImdbFilmeDTO mapFilme(String codigo, JsonNode title) {
        if (isMissing(title)) {
            throw new TitleNotFoundException(codigo);
        }
        String tituloLocal = text(title.path("titleText").path("text"));
        if (isBlank(tituloLocal)) {
            throw new TitleNotFoundException(codigo);
        }
        Money us = money(title.path("bilheteriaUS").path("total"));
        Money mundo = money(title.path("bilheteriaMundo").path("total"));

        return new ImdbFilmeDTO(
                codigo,
                firstNonBlank(text(title.path("originalTitleText").path("text")), tituloLocal),
                tituloLocal,
                text(title.path("plot").path("plotText").path("plainText")),
                ano(title.path("releaseYear")),
                parseDataComponents(title.path("releaseDate")),
                minutos(title.path("runtime")),
                text(title.path("primaryImage").path("url")),
                us.currency(), us.amount(),
                mundo.currency(), mundo.amount(),
                generos(title.path("genres").path("genres")),
                pessoas(title.path("diretores"), ImdbDiretor::new),
                pessoas(title.path("escritores"), ImdbEscritor::new),
                atores(title.path("elenco")),
                pessoas(title.path("produtores"), ImdbProdutor::new));
    }

    // ===================== ESTREIAS (comingSoon) =====================

    /** @param comingSoon nó {@code data.comingSoon} da resposta GraphQL */
    public List<ImdbEstreiaDTO> mapEstreias(JsonNode comingSoon) {
        List<ImdbEstreiaDTO> estreias = new ArrayList<>();
        for (JsonNode edge : comingSoon.path("edges")) {
            JsonNode n = edge.path("node");
            String codigo = text(n.path("id"));
            if (isBlank(codigo)) {
                continue;
            }
            estreias.add(new ImdbEstreiaDTO(
                    codigo,
                    text(n.path("titleText").path("text")),
                    text(n.path("titleType").path("text")),
                    ano(n.path("releaseYear")),
                    parseDataComponents(n.path("releaseDate")),
                    text(n.path("primaryImage").path("url")),
                    generosNomes(n.path("genres").path("genres")),
                    principais(n.path("principalCredits"))));
        }
        return estreias;
    }

    // ===================== PESSOA =====================

    /** @param name nó {@code data.name} da resposta GraphQL */
    public ImdbPessoaDTO mapPessoa(String codigo, JsonNode name) {
        String nome = text(name.path("nameText").path("text"));
        if (isMissing(name) || isBlank(nome)) {
            throw new PessoaNaoEncontradaException(codigo);
        }
        return new ImdbPessoaDTO(
                codigo,
                nome,
                text(name.path("primaryImage").path("url")),
                profissoes(name.path("primaryProfessions")),
                text(name.path("bio").path("text").path("plainText")),
                parseDataIso(text(name.path("birthDate").path("date"))),
                parseDataIso(text(name.path("deathDate").path("date"))));
    }

    // ===================== helpers de créditos =====================

    /** Lista de pessoas (código + nome) de uma connection {@code credits{edges{node{name}}}}. */
    private static <T> List<T> pessoas(JsonNode connection, BiFunction<String, String, T> construtor) {
        List<T> out = new ArrayList<>();
        for (JsonNode edge : connection.path("edges")) {
            JsonNode name = edge.path("node").path("name");
            String nome = text(name.path("nameText").path("text"));
            if (!isBlank(nome)) {
                out.add(construtor.apply(text(name.path("id")), nome));
            }
        }
        return out;
    }

    /** Elenco com personagens e ordem de billing (1 = primeiro do elenco). */
    private static List<ImdbAtor> atores(JsonNode connection) {
        List<ImdbAtor> atores = new ArrayList<>();
        int ordem = 1;
        for (JsonNode edge : connection.path("edges")) {
            JsonNode node = edge.path("node");
            JsonNode name = node.path("name");
            String nome = text(name.path("nameText").path("text"));
            if (isBlank(nome)) {
                continue;
            }
            atores.add(new ImdbAtor(text(name.path("id")), nome, personagens(node.path("characters")), ordem++));
        }
        return atores;
    }

    private static List<String> personagens(JsonNode characters) {
        List<String> out = new ArrayList<>();
        for (JsonNode c : characters) {
            String nome = text(c.path("name"));
            if (!isBlank(nome)) {
                out.add(nome);
            }
        }
        return out;
    }

    /** Achata os grupos de {@code principalCredits} em pessoas (resumo), sem repetir. */
    private static List<ImdbPessoaResumo> principais(JsonNode principalCredits) {
        List<ImdbPessoaResumo> out = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (JsonNode grupo : principalCredits) {
            for (JsonNode credit : grupo.path("credits")) {
                JsonNode name = credit.path("name");
                String nome = text(name.path("nameText").path("text"));
                String id = text(name.path("id"));
                if (isBlank(nome) || (id != null && !vistos.add(id))) {
                    continue;
                }
                out.add(new ImdbPessoaResumo(id, nome));
            }
        }
        return out;
    }

    // ===================== helpers de campos =====================

    private static List<ImdbGenero> generos(JsonNode arr) {
        List<ImdbGenero> out = new ArrayList<>();
        for (JsonNode g : arr) {
            String nome = text(g.path("text"));
            if (!isBlank(nome)) {
                out.add(new ImdbGenero(firstNonBlank(text(g.path("id")), nome), nome));
            }
        }
        return out;
    }

    private static List<String> generosNomes(JsonNode arr) {
        List<String> out = new ArrayList<>();
        for (JsonNode g : arr) {
            String nome = text(g.path("text"));
            if (!isBlank(nome)) {
                out.add(nome);
            }
        }
        return out;
    }

    private static List<String> profissoes(JsonNode arr) {
        List<String> out = new ArrayList<>();
        for (JsonNode p : arr) {
            String v = text(p.path("category").path("text"));
            if (!isBlank(v)) {
                out.add(v);
            }
        }
        return out;
    }

    private static Money money(JsonNode total) {
        if (isMissing(total)) {
            return new Money(null, null);
        }
        JsonNode amount = total.path("amount");
        return new Money(text(total.path("currency")), amount.isNumber() ? amount.decimalValue() : null);
    }

    private record Money(String currency, BigDecimal amount) {
    }

    private static String ano(JsonNode releaseYear) {
        JsonNode year = releaseYear.path("year");
        return year.isNumber() ? String.valueOf(year.asInt()) : null;
    }

    private static Integer minutos(JsonNode runtime) {
        JsonNode seconds = runtime.path("seconds");
        return seconds.isNumber() ? seconds.asInt() / 60 : null;
    }

    // ===================== helpers de data =====================

    private static LocalDate parseDataComponents(JsonNode releaseDate) {
        JsonNode d = releaseDate.path("day");
        JsonNode m = releaseDate.path("month");
        JsonNode y = releaseDate.path("year");
        if (!d.isNumber() || !m.isNumber() || !y.isNumber()) {
            return null;
        }
        try {
            return LocalDate.of(y.asInt(), m.asInt(), d.asInt());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalDate parseDataIso(String iso) {
        if (isBlank(iso)) {
            return null;
        }
        try {
            return LocalDate.parse(iso);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ===================== helpers gerais =====================

    private static boolean isMissing(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull();
    }

    private static String text(JsonNode node) {
        return isMissing(node) ? null : node.asText();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : null);
    }
}
