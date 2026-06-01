package com.sharcky.klientt.busca.dto;

import com.sharcky.klientt.busca.job.EstadoJob;

import java.util.List;

/**
 * Estado atual de uma busca + leads (quando concluída). Consumido pelo polling.
 */
public record ResultadoBusca(
        Long jobId,
        String termo,
        EstadoJob estado,
        List<LeadResponse> leads,
        String erro
) {
    public boolean concluido() {
        return estado == EstadoJob.CONCLUIDO;
    }

    public boolean falhou() {
        return estado == EstadoJob.ERRO;
    }

    public boolean terminado() {
        return concluido() || falhou();
    }
}
