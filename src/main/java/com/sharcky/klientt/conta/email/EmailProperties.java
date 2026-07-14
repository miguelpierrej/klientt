package com.sharcky.klientt.conta.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração do envio de email (Resend). Desligado por default: sem {@code enabled}+{@code api-key},
 * os emails não são enviados — o link de confirmação é apenas registado no log (útil em dev).
 */
@Component
@ConfigurationProperties(prefix = "klientt.email")
@Getter
@Setter
public class EmailProperties {

    /** Liga o envio real via Resend (requer api-key + from). */
    private boolean enabled = false;

    /** API key do Resend (re_...). */
    private String apiKey = "";

    /** Remetente. Ex.: "Klientt <nao-responder@teu-dominio.com>" ou "onboarding@resend.dev" (sandbox). */
    private String from = "Klientt <onboarding@resend.dev>";

    /** URL base da API do Resend. */
    private String baseUrl = "https://api.resend.com";

    public boolean isConfigurado() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
