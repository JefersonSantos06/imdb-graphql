package com.jsantos.imdbgraphql.imdb.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados de uma pessoa do IMDB ({@code /name/{nm}/}). Supera o legado (que só
 * trazia código + nome) incluindo foto, profissões, biografia e datas.
 */
public record ImdbPessoaDTO(
        String codigo,
        String nome,
        String foto,
        List<String> profissoes,
        String biografia,
        LocalDate dataNascimento,
        LocalDate dataFalecimento) {
}
