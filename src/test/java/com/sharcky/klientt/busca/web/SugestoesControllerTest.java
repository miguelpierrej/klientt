package com.sharcky.klientt.busca.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SugestoesControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void cidadeSugereMunicipioComUf() throws Exception {
        mvc.perform(get("/sugestoes/cidade").param("regiao", "bau").with(user("dev@klientt.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bauru")))
                .andExpect(content().string(containsString("sug-item")));
    }

    @Test
    void queryCurtaNaoSugereNada() throws Exception {
        mvc.perform(get("/sugestoes/cidade").param("regiao", "b").with(user("dev@klientt.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sug-item"))));
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/sugestoes/cidade").param("regiao", "bau"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
