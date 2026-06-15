package com.jsantos.imdbgraphql.imdb.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Linha de cache genérica: um documento JSON ({@code payload}) por
 * (tipo, chave, locale), com {@code fetchedAt} para o cálculo do TTL.
 * Ex.: tipo={@code FILME} chave={@code tt0111161}, ou tipo={@code PESSOA} chave={@code nm0000151}.
 */
@Entity
@Table(name = "imdb_cache",
        uniqueConstraints = @UniqueConstraint(name = "uq_imdb_cache",
                columnNames = {"tipo", "chave", "locale"}))
public class ImdbCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String chave;

    @Column(nullable = false, length = 20)
    private String locale;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ImdbCacheEntry() {
        // JPA
    }

    public ImdbCacheEntry(String tipo, String chave, String locale, String payload, Instant fetchedAt) {
        this.tipo = tipo;
        this.chave = chave;
        this.locale = locale;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getChave() {
        return chave;
    }

    public String getLocale() {
        return locale;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
