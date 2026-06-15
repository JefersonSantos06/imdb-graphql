package com.jsantos.imdbgraphql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracoes do wrapper do IMDB (prefixo {@code app.imdb}).
 */
@ConfigurationProperties(prefix = "app.imdb")
public class ImdbProperties {

    /** TTL padrao do cache; sobrescrito por requisicao via {@code ?ttl=<segundos>}. */
    private Duration cacheTtl = Duration.ofDays(7);

    /** Locale padrao para titulo local / sinopse quando nao informado na requisicao. */
    private String defaultLocale = "pt-BR";

    /** Timeout de conexao/requisicao da chamada GraphQL ao IMDB. */
    private Duration timeout = Duration.ofSeconds(30);

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
