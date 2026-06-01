package com.sharcky.klientt.conta.dto;

/**
 * Resumo da conta para a página /conta: dados do utilizador, plano e consumo do mês.
 */
public record ResumoConta(
        String nome,
        String email,
        String planoNome,
        int limiteLeadsMes,
        long consumoMes,
        long restante
) {
    /** Percentagem da cota usada (0–100), para a barra de progresso. */
    public int percentagemUsada() {
        if (limiteLeadsMes <= 0) {
            return 100;
        }
        long pct = Math.round(100.0 * consumoMes / limiteLeadsMes);
        return (int) Math.min(100, pct);
    }
}
