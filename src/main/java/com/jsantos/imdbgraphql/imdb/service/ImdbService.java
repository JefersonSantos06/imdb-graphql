package com.jsantos.imdbgraphql.imdb.service;

import com.jsantos.imdbgraphql.config.ImdbProperties;
import com.jsantos.imdbgraphql.imdb.dto.ImdbEstreiaDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbFilmeDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbPessoaDTO;
import com.jsantos.imdbgraphql.imdb.model.ImdbCacheEntry;
import com.jsantos.imdbgraphql.imdb.repository.ImdbCacheRepository;
import com.jsantos.imdbgraphql.imdb.scraper.ImdbScrapeException;
import com.jsantos.imdbgraphql.imdb.scraper.ImdbScraper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Orquestra cache + TTL + scraping. O cache é genérico por (tipo, chave, locale);
 * quando expira (ou {@code ttl=0}) re-raspa e atualiza a linha. As estreias
 * (calendário) não são cacheadas por serem sensíveis ao tempo.
 */
@Service
public class ImdbService {

    private static final String TIPO_FILME = "FILME";
    private static final String TIPO_PESSOA = "PESSOA";

    private final ImdbCacheRepository repository;
    private final ImdbScraper scraper;
    private final ImdbProperties properties;
    private final ObjectMapper objectMapper;

    public ImdbService(ImdbCacheRepository repository, ImdbScraper scraper,
                       ImdbProperties properties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.scraper = scraper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImdbFilmeDTO buscarFilme(String codigo, String localeParam, Long ttlSegundos) {
        String locale = resolveLocale(localeParam);
        return obterComCache(TIPO_FILME, codigo, locale, ttlSegundos, ImdbFilmeDTO.class,
                () -> scraper.scrape(codigo, locale));
    }

    @Transactional
    public ImdbPessoaDTO buscarPessoa(String nm, String localeParam, Long ttlSegundos) {
        String locale = resolveLocale(localeParam);
        return obterComCache(TIPO_PESSOA, nm, locale, ttlSegundos, ImdbPessoaDTO.class,
                () -> scraper.scrapePessoa(nm, locale));
    }

    public List<ImdbEstreiaDTO> buscarEstreias(String localeParam) {
        return scraper.scrapeEstreias(resolveLocale(localeParam));
    }

    private String resolveLocale(String localeParam) {
        return (localeParam == null || localeParam.isBlank()) ? properties.getDefaultLocale() : localeParam;
    }

    private <T> T obterComCache(String tipo, String chave, String locale, Long ttlSegundos,
                                Class<T> classe, Supplier<T> carregar) {
        Duration ttl = ttlSegundos != null
                ? Duration.ofSeconds(Math.max(0, ttlSegundos))
                : properties.getCacheTtl();

        Optional<ImdbCacheEntry> cached = repository.findByTipoAndChaveAndLocale(tipo, chave, locale);
        if (cached.isPresent() && !ttl.isZero() && isFresh(cached.get(), ttl)) {
            return deserialize(cached.get().getPayload(), classe);
        }

        T dto = carregar.get();
        upsert(cached.orElse(null), tipo, chave, locale, dto);
        return dto;
    }

    private static boolean isFresh(ImdbCacheEntry entry, Duration ttl) {
        return Duration.between(entry.getFetchedAt(), Instant.now()).compareTo(ttl) <= 0;
    }

    private void upsert(ImdbCacheEntry existing, String tipo, String chave, String locale, Object dto) {
        String payload = serialize(dto);
        Instant now = Instant.now();
        if (existing != null) {
            existing.setPayload(payload);
            existing.setFetchedAt(now);
            repository.save(existing);
        } else {
            repository.save(new ImdbCacheEntry(tipo, chave, locale, payload, now));
        }
    }

    private String serialize(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (RuntimeException e) {
            throw new ImdbScrapeException("Falha ao serializar recurso IMDB", e);
        }
    }

    private <T> T deserialize(String payload, Class<T> classe) {
        try {
            return objectMapper.readValue(payload, classe);
        } catch (RuntimeException e) {
            throw new ImdbScrapeException("Falha ao ler o cache IMDB", e);
        }
    }
}
