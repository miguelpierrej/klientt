package com.sharcky.klientt.enriquecimento.web;

import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoCallback;
import com.sharcky.klientt.enriquecimento.service.EnriquecimentoService;
import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook que recebe o callback de enriquecimento Maps (por empresa/CNPJ) — Fase 2.
 * Caminho em klientt.scraper.enriquecimento-callback-path. Autenticado por X-Klientt-Token.
 */
@RestController
public class EnriquecimentoCallbackController {

    private final EnriquecimentoService enriquecimentoService;
    private final ScraperProperties properties;

    public EnriquecimentoCallbackController(EnriquecimentoService enriquecimentoService,
                                            ScraperProperties properties) {
        this.enriquecimentoService = enriquecimentoService;
        this.properties = properties;
    }

    @PostMapping("${klientt.scraper.enriquecimento-callback-path}")
    public ResponseEntity<?> receber(
            @RequestHeader(value = ScraperClient.TOKEN_HEADER, required = false) String token,
            @RequestBody EnriquecimentoCallback callback) {

        if (!properties.getToken().equals(token)) {
            return ResponseEntity.status(401).body(Map.of("erro", "token inválido"));
        }
        enriquecimentoService.aplicar(callback);
        return ResponseEntity.ok(Map.of("estado", "recebido"));
    }
}
