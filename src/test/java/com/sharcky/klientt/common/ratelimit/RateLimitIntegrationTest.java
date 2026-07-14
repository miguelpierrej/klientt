package com.sharcky.klientt.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "klientt.ratelimit.enabled=true")
class RateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void loginRepetidoAcabaEmTooManyRequests() throws Exception {
        // Política de login: 12 por 5 min. Os primeiros 12 consomem os tokens; o 13º → 429.
        for (int i = 0; i < 12; i++) {
            mvc.perform(post("/login").param("username", "x@x.com").param("password", "errada").with(csrf()));
        }
        mvc.perform(post("/login").param("username", "x@x.com").param("password", "errada").with(csrf()))
                .andExpect(status().isTooManyRequests());
    }
}
