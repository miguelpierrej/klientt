package com.sharcky.klientt.pagamento.service;

/** Compra de créditos de leads via Stripe (pagamento único, Embedded Checkout). */
public interface PagamentoService {

    /** Stripe está configurado (há chave)? */
    boolean disponivel();

    /** Cria uma Checkout Session embutida e devolve o {@code clientSecret} para o Stripe.js. */
    String criarCheckout(Long utilizadorId, String returnUrlBase);

    /**
     * Confirma uma sessão de checkout no regresso do utilizador (via {@code session_id}) e credita
     * os leads se estiver paga (idempotente). É o caminho principal de fulfillment — não depende do
     * webhook. Devolve os leads creditados nesta chamada (0 se já processada, não paga, ou de outro
     * utilizador).
     */
    int confirmarPagamento(String sessionId, Long utilizadorId);

    /** Processa um webhook Stripe: em pagamento concluído, credita os leads (idempotente). Reforço. */
    void processarWebhook(String payload, String assinatura);
}
