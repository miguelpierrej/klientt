package com.sharcky.klientt.pagamento.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração da integração Stripe (klientt.stripe.*).
 * Sem uma secret key real (sk_...), a integração fica em "modo desligado":
 * a app arranca e o resto funciona; o pagamento mostra aviso.
 */
@Component
@ConfigurationProperties(prefix = "klientt.stripe")
@Getter
@Setter
public class StripeProperties {

    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";
    private String currency = "brl";

    /** Mapa nome-do-plano → Stripe Price ID (ex.: Pro → price_123). */
    private Map<String, String> prices = new HashMap<>();

    /** Considera-se ligado quando há uma secret key Stripe válida. */
    public boolean isEnabled() {
        return secretKey != null && secretKey.startsWith("sk_");
    }

    public String priceId(String plano) {
        return prices.get(plano);
    }

    /** Plano correspondente a um Price ID (reverso do mapa) — usado no webhook. */
    public String planoDoPrice(String priceId) {
        return prices.entrySet().stream()
                .filter(e -> e.getValue().equals(priceId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
