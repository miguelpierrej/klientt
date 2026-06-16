package com.sharcky.klientt.scraper.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o contexto arranca com o stub ligado (klientt.scraper.stub=true) — i.e. que o
 * StubScraperClient injeta o TaskExecutor sem ambiguidade (regressão do @EnableAsync, que trouxe
 * o applicationTaskExecutor além do taskScheduler do @EnableScheduling).
 */
@SpringBootTest(properties = "klientt.scraper.stub=true")
class StubContextLoadsTest {

    @Autowired
    ScraperClient scraperClient;

    @Test
    void arrancaComStubLigado() {
        assertThat(scraperClient).isInstanceOf(StubScraperClient.class);
    }
}
