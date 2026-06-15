package com.jsantos.imdbgraphql.imdb.scraper;

/** Lançada quando o código IMDB não corresponde a nenhuma pessoa. */
public class PessoaNaoEncontradaException extends RuntimeException {

    public PessoaNaoEncontradaException(String codigo) {
        super("Pessoa IMDB não encontrada: " + codigo);
    }
}
