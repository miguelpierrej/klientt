package com.sharcky.klientt.cnae.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da resolução nicho→CNAE via LLM (PLANO-DUAL-FONTE.md, Fase D).
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

    /** Chave da API Anthropic (ANTHROPIC_API_KEY). */
    private String apiKey = "";

    /** Modelo Claude a usar (default Opus 4.8; haiku para custo mais baixo). */
    private String model = "claude-opus-4-8";
}
