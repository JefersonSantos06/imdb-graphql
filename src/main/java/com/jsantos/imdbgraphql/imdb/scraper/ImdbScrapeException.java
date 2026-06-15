package com.jsantos.imdbgraphql.imdb.scraper;

/** Falha generica ao raspar/parsear uma pagina do IMDB. */
public class ImdbScrapeException extends RuntimeException {

    public ImdbScrapeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImdbScrapeException(String message) {
        super(message);
    }
}
