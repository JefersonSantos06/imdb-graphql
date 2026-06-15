package com.jsantos.imdbgraphql.imdb.dto;

/** Resumo de uma pessoa (código IMDB {@code nm...} + nome). */
public record ImdbPessoaResumo(String codigo, String nome) {
}
