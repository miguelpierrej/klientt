package com.sharcky.klientt.conta.service;

/**
 * Créditos de leads (pré-pagos, Stripe). Modelo "1ª página grátis": a 1ª página de cada busca não
 * consome créditos; ver mais ("carregar mais") consome 1 por empresa entregue além da 1ª página.
 */
public interface CreditosService {

    /** Total de leads comprados (saldo bruto). */
    long comprado(Long utilizadorId);

    /** Leads já entregues além da 1ª página (consumo). */
    long consumido(Long utilizadorId);

    /** Disponível = comprado − consumido (≥ 0). */
    long disponivel(Long utilizadorId);

    boolean temDisponivel(Long utilizadorId);

    /** Soma créditos ao saldo do utilizador (após pagamento). */
    void creditar(Long utilizadorId, int leads);
}
