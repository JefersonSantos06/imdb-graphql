package com.jsantos.imdbgraphql.imdb.dto;

import java.util.List;

/**
 * {@code codigo} = id IMDB da pessoa (ex.: {@code nm0000209});
 * {@code personagens} = personagem(ns) interpretado(s);
 * {@code ordemImportacao} = ordem de billing (1 = primeiro do elenco).
 */
public record ImdbAtor(String codigo, String nome, List<String> personagens, Integer ordemImportacao) {
}
