package com.sharcky.klientt.enriquecimento.web;

import com.sharcky.klientt.enriquecimento.EnriquecimentoService;
import com.sharcky.klientt.enriquecimento.config.ScraperProperties;
import com.sharcky.klientt.enriquecimento.dto.EnrichCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe os callbacks do scraper com as empresas enriquecidas. Autenticado pelo token partilhado
 * (X-Klientt-Token) — por isso é público na SecurityConfig e isento de CSRF (serviço externo,
 * sem sessão). Cada lote é idempotente (merge por CNPJ).
 */
@RestController
public class ScraperCallbackController {

    private final EnriquecimentoService enriquecimentoService;
    private final ScraperProperties properties;

    public ScraperCallbackController(EnriquecimentoService enriquecimentoService, ScraperProperties properties) {
        this.enriquecimentoService = enriquecimentoService;
        this.properties = properties;
    }

    @PostMapping("/api/scraper/callback")
    public ResponseEntity<String> callback(@RequestBody EnrichCallback callback,
                                           @RequestHeader(value = "X-Klientt-Token", required = false) String token) {
        if (!properties.getToken().equals(token)) {
            return ResponseEntity.status(401).body("token inválido");
        }
        enriquecimentoService.aplicar(callback);
        return ResponseEntity.ok("ok");
    }
}
