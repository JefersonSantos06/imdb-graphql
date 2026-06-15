package com.jsantos.imdbgraphql.imdb.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Item do calendário de estreias do IMDB ({@code /calendar/}). Traz o resumo já
 * presente no calendário — para os dados completos, consultar {@code /api/imdb/{codigo}}.
 */
public record ImdbEstreiaDTO(
        String codigo,
        String titulo,
        String tipo,
        String anoLancamento,
        LocalDate dataLancamento,
        String capa,
        List<String> generos,
        List<ImdbPessoaResumo> principais) {
}
