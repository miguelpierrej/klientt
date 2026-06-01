package com.sharcky.klientt.busca.scoring;

/**
 * Resultado da avaliação de um lead: sinais derivados + score de oportunidade.
 * Value object imutável calculado pelo {@link AvaliadorLead}.
 */
public record AvaliacaoLead(
        double notaGoogle,
        boolean temSite,
        boolean siteLento,
        int seguidores,
        boolean proconEviteSite,
        int score
) {
}
