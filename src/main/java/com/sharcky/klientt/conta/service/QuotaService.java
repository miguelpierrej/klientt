package com.sharcky.klientt.conta.service;

/**
 * Controla a cota mensal de leads por plano.
 */
public interface QuotaService {

    /** Garante que o utilizador ainda tem cota; lança {@link QuotaExcedidaException} caso contrário. */
    void garantirDisponibilidade(Long utilizadorId);

    /** Leads consumidos pelo utilizador no mês corrente. */
    long consumoMesAtual(Long utilizadorId);
}
