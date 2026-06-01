package com.sharcky.klientt.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da integração com o serviço de scraping (CONTRATO-SCRAPER.md §5).
 */
@Component
@ConfigurationProperties(prefix = "klientt.scraper")
@Getter
@Setter
public class ScraperProperties {

    /** URL base do serviço de scraping. */
    private String baseUrl = "http://localhost:8000";

    /** Segredo partilhado enviado/validado no header X-Klientt-Token. */
    private String token = "dev-secret";

    /** Se true, usa o StubScraperClient (sem scraper real). */
    private boolean stub = true;

    /** Base pública do Klientt, para montar o callbackUrl. */
    private String publicBaseUrl = "http://localhost:8080";

    /** Caminho do webhook que recebe o callback do scraper. */
    private String callbackPath = "/api/scraper/callbacks";

    /** Limite de empresas por busca. */
    private int limiteDefault = 50;

    public String callbackUrl() {
        return publicBaseUrl + callbackPath;
    }
}
