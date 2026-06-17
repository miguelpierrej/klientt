package com.sharcky.klientt.conta.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void homeSemLoginRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void paginaDeLoginEPublica() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void faviconEPublico() throws Exception {
        mvc.perform(get("/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void rotaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/rota-inexistente").with(user("teste")))
                .andExpect(status().isNotFound());
    }

    @Test
    void credenciaisErradasNaoAutenticam() throws Exception {
        mvc.perform(formLogin("/login").user("dev@klientt.com").password("errada"))
                .andExpect(unauthenticated());
    }

    @Test
    void loginValidoDaAcessoAContaComOPlano() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(
                        formLogin("/login").user("dev@klientt.com").password("dev12345"))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();

        mvc.perform(get("/conta").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agency")));   // utilizador de dev: plano Agency
    }
}
