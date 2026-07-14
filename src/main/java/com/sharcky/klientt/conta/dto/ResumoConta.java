package com.sharcky.klientt.conta.dto;

/**
 * Resumo da conta para /conta: dados do utilizador + saldo de créditos de leads.
 */
public record ResumoConta(
        String nome,
        String email,
        long comprado,
        long consumido,
        long disponivel
) {
    /** Porcentagem de créditos usada (0–100), para a barra de progresso. */
    public int percentagemUsada() {
        if (comprado <= 0) {
            return 0;
        }
        long pct = Math.round(100.0 * consumido / comprado);
        return (int) Math.max(0, Math.min(100, pct));
    }
}
