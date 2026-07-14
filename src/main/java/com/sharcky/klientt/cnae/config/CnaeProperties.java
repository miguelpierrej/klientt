package com.sharcky.klientt.cnae.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da resolução nicho→CNAE via LLM (Gemini).
 * Por default desligada: sem chave, a app arranca e a resolução fica limitada à
 * tabela determinística (sem fallback de LLM).
 */
@Component
@ConfigurationProperties(prefix = "klientt.cnae")
@Getter
@Setter
public class CnaeProperties {

    /** Liga o fallback por LLM (requer api-key). */
    private boolean enabled = false;

    /** Chave da API Gemini (GEMINI_API_KEY). */
    private String apiKey = "";

    /** Modelo Gemini a usar (flash = baixo custo/latência). */
    private String model = "gemini-2.5-flash";

    /** URL base da Generative Language API (troca só para proxy/testes). */
    private String baseUrl = "https://generativelanguage.googleapis.com";
}
