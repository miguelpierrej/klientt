package com.sharcky.klientt.cnae;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com o fallback LLM ligado (klientt.cnae.enabled=true), o contexto tem de arrancar e criar o
 * {@link GeminiTradutorCnaeLlm} — que não depende de nenhum bean ObjectMapper (o app corre em
 * Jackson 3). Guarda contra a regressão "required a bean of type ObjectMapper".
 */
@SpringBootTest
@TestPropertySource(properties = {
        "klientt.cnae.enabled=true",
        "klientt.cnae.api-key=chave-dummy-de-teste"
})
class GeminiTradutorCnaeLlmContextTest {

    @Autowired(required = false)
    TradutorCnaeLlm tradutor;

    @Test
    void contextoArrancaComFallbackLlmLigado() {
        assertThat(tradutor).isInstanceOf(GeminiTradutorCnaeLlm.class);
    }
}
