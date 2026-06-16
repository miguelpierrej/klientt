package com.sharcky.klientt.pagamento.service;

import com.sharcky.klientt.pagamento.dto.SubscricaoIntent;

/**
 * Subscrições via Stripe.
 */
public interface PagamentoService {

    /** Stripe está configurado (há secret key). */
    boolean disponivel();

    /** Cria/atualiza a subscrição do utilizador e devolve o que o frontend precisa para pagar. */
    SubscricaoIntent iniciarSubscricao(Long utilizadorId, String planoNome);

    /** Processa um evento de webhook da Stripe (atualiza o plano do utilizador). */
    void processarWebhook(String payload, String assinatura);
}
