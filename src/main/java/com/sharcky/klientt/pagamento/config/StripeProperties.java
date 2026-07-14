package com.sharcky.klientt.pagamento.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração Stripe (klientt.stripe.*) — compra de créditos de leads (pagamento único).
 * Sem uma chave válida ({@code sk_}/{@code rk_}), fica em modo desligado (a app arranca; o botão
 * de compra mostra "indisponível").
 */
@Component
@ConfigurationProperties(prefix = "klientt.stripe")
@Getter
@Setter
public class StripeProperties {

    /** Chave secreta ou restrita (recomendado RAK, {@code rk_}). */
    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";

    /** Price ID do pacote de créditos (criado na dashboard Stripe). */
    private String priceId = "";

    /** Leads que cada compra concede. */
    private int leadsPorPacote = 3000;

    /** Considera-se ligado quando há uma chave secreta/restrita. */
    public boolean isEnabled() {
        return secretKey != null && (secretKey.startsWith("sk_") || secretKey.startsWith("rk_"));
    }
}
