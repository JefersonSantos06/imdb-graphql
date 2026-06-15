package com.jsantos.imdbgraphql.imdb.repository;

import com.jsantos.imdbgraphql.imdb.model.ImdbCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImdbCacheRepository extends JpaRepository<ImdbCacheEntry, Long> {

    Optional<ImdbCacheEntry> findByTipoAndChaveAndLocale(String tipo, String chave, String locale);
}
