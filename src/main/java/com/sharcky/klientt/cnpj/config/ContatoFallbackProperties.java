package com.sharcky.klientt.cnpj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração do fallback de contacto por CNPJ via API pública (BrasilAPI) — PLANO-SO-API.md, Fase D.
 * Desligado por default: sem isto, a app não consulta a API pública (a descoberta basta).
 */
@Component
@ConfigurationProperties(prefix = "klientt.contato-fallback")
@Getter
@Setter
public class ContatoFallbackProperties {

    /** Liga o fallback de contacto por CNPJ. */
    private boolean enabled = false;

    /** URL base da API pública de CNPJ. */
    private String baseUrl = "https://brasilapi.com.br";
}
