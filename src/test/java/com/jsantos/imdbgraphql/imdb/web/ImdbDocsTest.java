package com.jsantos.imdbgraphql.imdb.web;

import com.jsantos.imdbgraphql.config.SecurityConfig;
import com.jsantos.imdbgraphql.imdb.dto.*;
import com.jsantos.imdbgraphql.imdb.service.ImdbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gera os snippets do Spring REST Docs (em target/generated-snippets) a partir de
 * chamadas reais aos endpoints. Os snippets são incluídos por src/docs/asciidoc/index.adoc.
 */
@WebMvcTest(ImdbController.class)
@AutoConfigureRestDocs
@Import(SecurityConfig.class)
class ImdbDocsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ImdbService service;

    @Test
    void documentaFilme() throws Exception {
        given(service.buscarFilme(any(), any(), any())).willReturn(filmeAmostra());

        mvc.perform(get("/api/imdb/{codigo}", "tt0111161")
                        .param("locale", "pt-BR")
                        .param("ttl", "86400")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andDo(document("filme",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("codigo").description("Código IMDB do título (formato ttNNNNNNN)")),
                        queryParameters(
                                parameterWithName("locale").optional()
                                        .description("Locale do título local e da sinopse. Default: pt-BR"),
                                parameterWithName("ttl").optional()
                                        .description("TTL do cache em segundos; 0 força novo scrape")),
                        responseFields(
                                fieldWithPath("codigo").description("Código IMDB do título"),
                                fieldWithPath("tituloOriginal").description("Título original"),
                                fieldWithPath("tituloLocal").description("Título no locale solicitado"),
                                fieldWithPath("sinopse").description("Sinopse no locale solicitado"),
                                fieldWithPath("anoLancamento").description("Ano de lançamento original do título"),
                                fieldWithPath("dataLancamento").description("Data de estreia (regional, conforme o locale)"),
                                fieldWithPath("duracaoEmMinutos").description("Duração em minutos"),
                                fieldWithPath("capa").description("URL da imagem de capa (pôster)"),
                                fieldWithPath("bilheteriaMoedaUS").description("Moeda da bilheteria EUA e Canadá"),
                                fieldWithPath("bilheteriaValorUS").description("Bilheteria acumulada EUA e Canadá"),
                                fieldWithPath("bilheteriaMoedaMundo").description("Moeda da bilheteria mundial"),
                                fieldWithPath("bilheteriaValorMundo").description("Bilheteria acumulada mundial"),
                                fieldWithPath("generos[].codigo").description("ID do gênero no IMDB"),
                                fieldWithPath("generos[].nome").description("Nome do gênero"),
                                fieldWithPath("diretores[].codigo").description("Código IMDB do diretor (nm...)"),
                                fieldWithPath("diretores[].nome").description("Nome do diretor"),
                                fieldWithPath("escritores[].codigo").description("Código IMDB do escritor (nm...)"),
                                fieldWithPath("escritores[].nome").description("Nome do escritor"),
                                fieldWithPath("atores[].codigo").description("Código IMDB do ator (nm...)"),
                                fieldWithPath("atores[].nome").description("Nome do ator"),
                                fieldWithPath("atores[].personagens").description("Personagens interpretados"),
                                fieldWithPath("atores[].ordemImportacao").description("Ordem de billing (1 = primeiro do elenco)"),
                                fieldWithPath("produtores[].codigo").description("Código IMDB do produtor (nm...)"),
                                fieldWithPath("produtores[].nome").description("Nome do produtor"))));
    }

    @Test
    void documentaPessoa() throws Exception {
        given(service.buscarPessoa(any(), any(), any())).willReturn(pessoaAmostra());

        mvc.perform(get("/api/imdb/pessoa/{nm}", "nm0000151")
                        .param("locale", "pt-BR")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andDo(document("pessoa",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("nm").description("Código IMDB da pessoa (formato nmNNNNNNN)")),
                        queryParameters(
                                parameterWithName("locale").optional()
                                        .description("Locale da biografia/profissões. Default: pt-BR"),
                                parameterWithName("ttl").optional()
                                        .description("TTL do cache em segundos; 0 força novo scrape")),
                        responseFields(
                                fieldWithPath("codigo").description("Código IMDB da pessoa"),
                                fieldWithPath("nome").description("Nome da pessoa"),
                                fieldWithPath("foto").description("URL da foto principal"),
                                fieldWithPath("profissoes").description("Profissões principais"),
                                fieldWithPath("biografia").description("Biografia no locale solicitado"),
                                fieldWithPath("dataNascimento").description("Data de nascimento"),
                                fieldWithPath("dataFalecimento").optional()
                                        .description("Data de falecimento; null se viva"))));
    }

    @Test
    void documentaEstreias() throws Exception {
        given(service.buscarEstreias(any())).willReturn(estreiasAmostra());

        mvc.perform(get("/api/imdb/estreias")
                        .param("locale", "pt-BR")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andDo(document("estreias",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("locale").optional()
                                        .description("Locale/região do calendário. Default: pt-BR")),
                        responseFields(
                                fieldWithPath("[].codigo").description("Código IMDB do título"),
                                fieldWithPath("[].titulo").description("Título no locale solicitado"),
                                fieldWithPath("[].tipo").description("Tipo (Movie, TV Series, ...)"),
                                fieldWithPath("[].anoLancamento").description("Ano de lançamento"),
                                fieldWithPath("[].dataLancamento").description("Data de estreia"),
                                fieldWithPath("[].capa").description("URL da imagem de capa"),
                                fieldWithPath("[].generos").description("Gêneros"),
                                fieldWithPath("[].principais[].codigo").description("Código IMDB da pessoa em destaque (nm...)"),
                                fieldWithPath("[].principais[].nome").description("Nome da pessoa em destaque"))));
    }

    // --- amostras ---

    private static ImdbFilmeDTO filmeAmostra() {
        return new ImdbFilmeDTO(
                "tt0111161", "The Shawshank Redemption", "Um Sonho de Liberdade",
                "Dois homens presos se reúnem ao longo de vários anos.", "1994",
                LocalDate.of(1994, 10, 14), 142, "https://m.media-amazon.com/images/M/shawshank.jpg",
                "USD", new BigDecimal("28767189"), "USD", new BigDecimal("29422415"),
                List.of(new ImdbGenero("Drama", "Drama")),
                List.of(new ImdbDiretor("nm0001104", "Frank Darabont")),
                List.of(new ImdbEscritor("nm0000175", "Stephen King")),
                List.of(new ImdbAtor("nm0000209", "Tim Robbins", List.of("Andy Dufresne"), 1)),
                List.of(new ImdbProdutor("nm0323065", "Liz Glotzer")));
    }

    private static ImdbPessoaDTO pessoaAmostra() {
        return new ImdbPessoaDTO(
                "nm0000151", "Morgan Freeman",
                "https://m.media-amazon.com/images/M/morgan.jpg",
                List.of("Ator", "Produção", "Direção"),
                "Ator norte-americano com voz marcante.",
                LocalDate.of(1937, 6, 1), null);
    }

    private static List<ImdbEstreiaDTO> estreiasAmostra() {
        return List.of(new ImdbEstreiaDTO(
                "tt39674832", "Diamond Made Man", "Movie", "2026",
                LocalDate.of(2026, 6, 16), "https://m.media-amazon.com/images/M/diamond.jpg",
                List.of("Action", "Sci-Fi"),
                List.of(new ImdbPessoaResumo("nm3440681", "Sreehari"))));
    }
}
