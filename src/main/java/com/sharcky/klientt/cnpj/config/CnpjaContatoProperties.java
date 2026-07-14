package com.sharcky.klientt.cnpj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Enriquecimento de contacto por CNPJ via <b>CNPJá open</b> (https://open.cnpja.com) — API pública
 * gratuita com contactos ricos ({@code emails[]}/{@code phones[]}). Fonte primária da cadeia de
 * contacto (fallback: BrasilAPI). Limite público: <b>5 req/min por IP</b> → ver {@code reqPorMinuto}.
 */
@Component
@ConfigurationProperties(prefix = "klientt.contato-cnpja")
@Getter
@Setter
public class CnpjaContatoProperties {

    private boolean enabled = true;

    private String baseUrl = "https://open.cnpja.com";

    /** Teto de pedidos por minuto (limite público = 5/min por IP). */
    private int reqPorMinuto = 5;
}
