package com.sharcky.klientt.pagamento.dto;

/**
 * Dados que o frontend (Stripe.js + Payment Element) precisa para confirmar o pagamento.
 */
public record SubscricaoIntent(
        String clientSecret,
        String publishableKey,
        String planoNome
) {
}
