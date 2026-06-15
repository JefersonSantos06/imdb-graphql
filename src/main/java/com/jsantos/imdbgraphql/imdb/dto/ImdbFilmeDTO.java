package com.jsantos.imdbgraphql.imdb.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representacao de um filme extraido do IMDB. Espelha os campos solicitados e
 * e o objeto serializado integralmente no cache (coluna {@code payload}).
 */
public record ImdbFilmeDTO(
        String codigo,
        String tituloOriginal,
        String tituloLocal,
        String sinopse,
        String anoLancamento,
        LocalDate dataLancamento,
        Integer duracaoEmMinutos,
        String capa,
        String bilheteriaMoedaUS,
        BigDecimal bilheteriaValorUS,
        String bilheteriaMoedaMundo,
        BigDecimal bilheteriaValorMundo,
        List<ImdbGenero> generos,
        List<ImdbDiretor> diretores,
        List<ImdbEscritor> escritores,
        List<ImdbAtor> atores,
        List<ImdbProdutor> produtores) {
}
