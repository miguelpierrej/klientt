package com.sharcky.klientt.scraper.web;

import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.ScrapeCallback;
import com.sharcky.klientt.scraper.ingest.ScrapeCallbackHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook que recebe o callback do scraper com os resultados (CONTRATO-SCRAPER.md §3).
 * O caminho é o configurado em klientt.scraper.callback-path.
 */
@RestController
public class ScraperCallbackController {

    private final ScrapeCallbackHandler callbackHandler;
    private final ScraperProperties properties;

    public ScraperCallbackController(ScrapeCallbackHandler callbackHandler, ScraperProperties properties) {
        this.callbackHandler = callbackHandler;
        this.properties = properties;
    }

    @PostMapping("${klientt.scraper.callback-path}")
    public ResponseEntity<?> receber(
            @RequestHeader(value = ScraperClient.TOKEN_HEADER, required = false) String token,
            @RequestBody ScrapeCallback callback) {

        if (!properties.getToken().equals(token)) {
            return ResponseEntity.status(401).body(Map.of("erro", "token inválido"));
        }

        callbackHandler.tratar(callback);
        return ResponseEntity.ok(Map.of("estado", "recebido"));
    }
}
