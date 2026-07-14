package com.sharcky.klientt.enriquecimento.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração do serviço de enriquecimento (scraper companheiro — ver 'Novo Fluxo.md').
 *
 * <p>Após a descoberta (Casa dos Dados), o Klientt envia a lista de empresas ao scraper
 * ({@code POST base-url/v1/enrich}) e recebe o resultado por callback em
 * {@code callback-base-url/api/scraper/callback}. Desligado por default: sem {@code enabled}
 * + {@code base-url}, o job conclui logo na descoberta (comportamento só-API).
 */
@Component
@ConfigurationProperties(prefix = "klientt.scraper")
@Getter
@Setter
public class ScraperProperties {

    /** Liga o enriquecimento por scraper (requer base-url + token). */
    private boolean enabled = false;

    /** URL base do serviço de scraping (ex.: http://localhost:8000). */
    private String baseUrl = "";

    /** Token partilhado, enviado/validado no header X-Klientt-Token. */
    private String token = "dev-secret";

    /** URL base pública deste app, para o scraper devolver o callback (ex.: http://localhost:8080). */
    private String callbackBaseUrl = "http://localhost:8080";

    /** Opções do pedido de enriquecimento. */
    private boolean coletarEmails = true;
    private boolean usarMaps = true;
    private boolean verificarSmtp = false;
    private int tamanhoLote = 15;

    public boolean isConfigurado() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }

    public String callbackUrl() {
        String base = callbackBaseUrl == null ? "" : callbackBaseUrl.replaceAll("/+$", "");
        return base + "/api/scraper/callback";
    }
}
