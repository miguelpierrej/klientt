package com.sharcky.klientt.cnpj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da fonte de descoberta por CNAE via API gerida de CNPJ
 * (CNPJá/Casa dos Dados — PLANO-DUAL-FONTE.md, Fase D).
 * Por default desligada: a app arranca e a busca por CNAE simplesmente não devolve nada.
 */
@Component
@ConfigurationProperties(prefix = "klientt.cnpj")
@Getter
@Setter
public class ClienteCnpjProperties {

    /** Liga a fonte (requer base-url + api-key do fornecedor). */
    private boolean enabled = false;

    /** URL base da API de CNPJ por CNAE. */
    private String baseUrl = "";

    /** Token/chave do fornecedor. */
    private String apiKey = "";

    /** Limite default de empresas por busca. */
    private int limiteDefault = 50;

    public boolean isConfigurado() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }
}
