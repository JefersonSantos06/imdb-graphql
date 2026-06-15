package com.jsantos.imdbgraphql.imdb.web;

import com.jsantos.imdbgraphql.imdb.dto.ImdbFilmeDTO;
import com.jsantos.imdbgraphql.imdb.service.ImdbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImdbSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ImdbService service;

    @Test
    void semCredenciais_retorna401() throws Exception {
        mvc.perform(get("/api/imdb/tt0111161"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comCredenciais_retorna200() throws Exception {
        given(service.buscarFilme(any(), any(), any())).willReturn(amostra());

        mvc.perform(get("/api/imdb/tt0111161").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("tt0111161"))
                .andExpect(jsonPath("$.tituloOriginal").value("The Shawshank Redemption"));
    }

    private static ImdbFilmeDTO amostra() {
        return new ImdbFilmeDTO("tt0111161", "The Shawshank Redemption", "Um Sonho de Liberdade",
                null, "1994", null, 142, null,
                "USD", null, "USD", null,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
