package com.jsantos.imdbgraphql.imdb.scraper;

import com.jsantos.imdbgraphql.imdb.dto.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImdbGraphqlMapperTest {

    private final ImdbGraphqlMapper mapper = new ImdbGraphqlMapper();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void extraiCamposDoFilme() {
        ImdbFilmeDTO dto = mapper.mapFilme("tt0111161", fixture("/imdb/title-graphql.json"));

        assertThat(dto.codigo()).isEqualTo("tt0111161");
        assertThat(dto.tituloLocal()).isEqualTo("Um Sonho de Liberdade");
        assertThat(dto.tituloOriginal()).isEqualTo("The Shawshank Redemption");
        assertThat(dto.anoLancamento()).isEqualTo("1994");
        assertThat(dto.dataLancamento()).isEqualTo(LocalDate.of(1994, 10, 14));
        assertThat(dto.duracaoEmMinutos()).isEqualTo(142);
        assertThat(dto.sinopse()).contains("banqueiro");
        assertThat(dto.capa()).isEqualTo("https://m.media-amazon.com/images/M/shawshank.jpg");
        assertThat(dto.bilheteriaMoedaUS()).isEqualTo("USD");
        assertThat(dto.bilheteriaValorUS()).isEqualByComparingTo("28767189");
        assertThat(dto.bilheteriaMoedaMundo()).isEqualTo("USD");
        assertThat(dto.bilheteriaValorMundo()).isEqualByComparingTo("29422415");
        assertThat(dto.generos()).extracting(ImdbGenero::nome).containsExactly("Drama");
    }

    @Test
    void extraiCreditosCompletosComPersonagensEOrdem() {
        ImdbFilmeDTO dto = mapper.mapFilme("tt0111161", fixture("/imdb/title-graphql.json"));

        assertThat(dto.diretores()).hasSize(1);
        assertThat(dto.diretores().get(0).codigo()).isEqualTo("nm0001104");
        assertThat(dto.diretores().get(0).nome()).isEqualTo("Frank Darabont");

        assertThat(dto.escritores()).hasSize(2);
        assertThat(dto.produtores()).extracting(ImdbProdutor::nome)
                .containsExactly("Liz Glotzer", "David Lester");

        assertThat(dto.atores()).hasSize(2);
        ImdbAtor primeiro = dto.atores().get(0);
        assertThat(primeiro.codigo()).isEqualTo("nm0000209");
        assertThat(primeiro.nome()).isEqualTo("Tim Robbins");
        assertThat(primeiro.personagens()).containsExactly("Andy Dufresne");
        assertThat(primeiro.ordemImportacao()).isEqualTo(1);
        assertThat(dto.atores().get(1).ordemImportacao()).isEqualTo(2);
    }

    @Test
    void filmeAusente_lancaNotFound() {
        assertThatThrownBy(() -> mapper.mapFilme("tt9999999", json.readTree("{}")))
                .isInstanceOf(TitleNotFoundException.class);
        assertThatThrownBy(() -> mapper.mapFilme("tt9999999", json.nullNode()))
                .isInstanceOf(TitleNotFoundException.class);
    }

    @Test
    void mapEstreias_extraiCalendario() {
        List<ImdbEstreiaDTO> estreias = mapper.mapEstreias(fixture("/imdb/comingsoon-graphql.json"));

        assertThat(estreias).hasSize(2);
        ImdbEstreiaDTO primeira = estreias.get(0);
        assertThat(primeira.codigo()).isEqualTo("tt39674832");
        assertThat(primeira.titulo()).isEqualTo("Diamond Made Man");
        assertThat(primeira.tipo()).isEqualTo("Movie");
        assertThat(primeira.anoLancamento()).isEqualTo("2026");
        assertThat(primeira.dataLancamento()).isEqualTo(LocalDate.of(2026, 6, 16));
        assertThat(primeira.capa()).isEqualTo("https://m.media-amazon.com/images/M/diamond.jpg");
        assertThat(primeira.generos()).containsExactly("Action", "Sci-Fi");
        assertThat(primeira.principais()).extracting("nome").containsExactly("Sreehari", "Vasudha");
    }

    @Test
    void mapPessoa_extraiDados() {
        ImdbPessoaDTO pessoa = mapper.mapPessoa("nm0000151", fixture("/imdb/name-graphql.json"));

        assertThat(pessoa.codigo()).isEqualTo("nm0000151");
        assertThat(pessoa.nome()).isEqualTo("Morgan Freeman");
        assertThat(pessoa.foto()).isEqualTo("https://m.media-amazon.com/images/M/morgan.jpg");
        assertThat(pessoa.profissoes()).containsExactly("Actor", "Producer", "Director");
        assertThat(pessoa.biografia()).contains("voz marcante");
        assertThat(pessoa.dataNascimento()).isEqualTo(LocalDate.of(1937, 6, 1));
        assertThat(pessoa.dataFalecimento()).isNull();
    }

    @Test
    void mapPessoa_semNome_lancaNaoEncontrada() {
        assertThatThrownBy(() -> mapper.mapPessoa("nm9999999", json.readTree("{}")))
                .isInstanceOf(PessoaNaoEncontradaException.class);
    }

    private JsonNode fixture(String path) {
        try (var in = ImdbGraphqlMapperTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("fixture nao encontrada: " + path);
            }
            return json.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
