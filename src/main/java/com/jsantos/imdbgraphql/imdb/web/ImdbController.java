package com.jsantos.imdbgraphql.imdb.web;

import com.jsantos.imdbgraphql.imdb.dto.ImdbEstreiaDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbFilmeDTO;
import com.jsantos.imdbgraphql.imdb.dto.ImdbPessoaDTO;
import com.jsantos.imdbgraphql.imdb.service.ImdbService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints do wrapper IMDB:
 * <ul>
 *   <li>{@code GET /api/imdb/{codigo}} — filme por código (ex.: tt0111161)</li>
 *   <li>{@code GET /api/imdb/estreias} — calendário de estreias</li>
 *   <li>{@code GET /api/imdb/pessoa/{nm}} — pessoa por código (ex.: nm0000151)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/imdb")
@Validated
public class ImdbController {

    private final ImdbService service;

    public ImdbController(ImdbService service) {
        this.service = service;
    }

    /**
     * @param codigo código IMDB do título ({@code ttNNNNNNN})
     * @param locale locale do título local / sinopse (opcional; default da configuração)
     * @param ttl    TTL do cache em segundos (opcional; {@code 0} força re-scrape)
     */
    @GetMapping("/{codigo}")
    public ImdbFilmeDTO buscarFilme(
            @PathVariable @Pattern(regexp = "tt\\d{7,8}",
                    message = "código IMDB inválido (esperado ttNNNNNNN)") String codigo,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Long ttl) {
        return service.buscarFilme(codigo, locale, ttl);
    }

    /**
     * Calendário de estreias do IMDB. Não usa cache (sensível ao tempo).
     *
     * @param locale locale/região do calendário (opcional; default da configuração)
     */
    @GetMapping("/estreias")
    public List<ImdbEstreiaDTO> buscarEstreias(@RequestParam(required = false) String locale) {
        return service.buscarEstreias(locale);
    }

    /**
     * @param nm     código IMDB da pessoa ({@code nmNNNNNNN})
     * @param locale locale (opcional; default da configuração)
     * @param ttl    TTL do cache em segundos (opcional; {@code 0} força re-scrape)
     */
    @GetMapping("/pessoa/{nm}")
    public ImdbPessoaDTO buscarPessoa(
            @PathVariable @Pattern(regexp = "nm\\d{7,8}",
                    message = "código IMDB inválido (esperado nmNNNNNNN)") String nm,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Long ttl) {
        return service.buscarPessoa(nm, locale, ttl);
    }
}
