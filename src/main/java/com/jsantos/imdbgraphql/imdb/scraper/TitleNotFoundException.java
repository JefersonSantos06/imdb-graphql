package com.jsantos.imdbgraphql.imdb.scraper;

/** Lancada quando o codigo IMDB nao corresponde a nenhum titulo. */
public class TitleNotFoundException extends RuntimeException {

    public TitleNotFoundException(String codigo) {
        super("Titulo IMDB nao encontrado: " + codigo);
    }
}
