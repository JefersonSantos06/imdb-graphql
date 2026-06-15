package com.jsantos.imdbgraphql.imdb.web;

import com.jsantos.imdbgraphql.imdb.scraper.ImdbScrapeException;
import com.jsantos.imdbgraphql.imdb.scraper.PessoaNaoEncontradaException;
import com.jsantos.imdbgraphql.imdb.scraper.TitleNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ImdbExceptionHandler {

    @ExceptionHandler({TitleNotFoundException.class, PessoaNaoEncontradaException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleValidation(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ImdbScrapeException.class)
    public ProblemDetail handleScrape(ImdbScrapeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
