package com.sharcky.klientt.conta.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void landingEPublica() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Klientt")));
    }

    @Test
    void appSemLoginRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void pedidoHtmxSemLoginDevolveHxRedirect() throws Exception {
        // Sessão expirada num pedido HTMX → HX-Redirect (recarrega a página inteira), não 302.
        mvc.perform(get("/app").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/login?expirou"));
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
    void robotsTxtEPublico() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap:")));
    }

    @Test
    void sitemapEPublico() throws Exception {
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<urlset")));
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
    void loginValidoDaAcessoAContaComCreditos() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(
                        formLogin("/login").user("dev@klientt.com").password("dev12345"))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();

        mvc.perform(get("/conta").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Créditos de leads")));   // /conta mostra saldo
    }

    @Test
    void appRenderizaParaUtilizadorAutenticado() throws Exception {
        // Renderiza a busca.html por inteiro → apanha erros de template (ex.: script sem th:inline).
        MockHttpSession session = (MockHttpSession) mvc.perform(
                        formLogin("/login").user("dev@klientt.com").password("dev12345"))
                .andReturn().getRequest().getSession();

        mvc.perform(get("/app").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O que procura?")));
    }

    @Test
    void onboardingRenderizaParaUtilizadorAutenticado() throws Exception {
        // Renderiza a onboarding.html por inteiro (prefill/projeções Thymeleaf, chips).
        MockHttpSession session = (MockHttpSession) mvc.perform(
                        formLogin("/login").user("dev@klientt.com").password("dev12345"))
                .andReturn().getRequest().getSession();

        mvc.perform(get("/onboarding").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Que atividades você quer alcançar?")));
    }

    @Test
    void submeterOnboardingSalvaERedirecionaParaApp() throws Exception {
        // Valida o binding do formulário (lista de portes + checkboxes booleanos + hidden CSV).
        MockHttpSession session = (MockHttpSession) mvc.perform(
                        formLogin("/login").user("dev@klientt.com").password("dev12345"))
                .andReturn().getRequest().getSession();

        mvc.perform(post("/onboarding").session(session).with(csrf())
                        .param("oferta", "criação de sites")
                        .param("nichosAlvo", "5611201")
                        .param("regioesAlvo", "Bauru/SP")
                        .param("portes", "MEI").param("portes", "MICRO")
                        .param("querComContato", "on")
                        .param("querSemSite", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app"));
    }
}
