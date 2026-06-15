package com.jsantos.imdbgraphql.imdb.dto;

/** {@code codigo} = id do gênero no IMDB (ex.: {@code Drama}). */
public record ImdbGenero(String codigo, String nome) {
}
