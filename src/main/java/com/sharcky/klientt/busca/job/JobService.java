package com.sharcky.klientt.busca.job;

import com.sharcky.klientt.busca.dto.BuscaRequest;

import java.util.Optional;

/**
 * Ciclo de vida dos jobs de busca.
 */
public interface JobService {

    /** Cria um job (do utilizador) em estado A_PROCESSAR e devolve o id (commit imediato). */
    Long criar(BuscaRequest request, Long utilizadorId);

    Optional<JobBusca> obter(Long jobId);

    /** Regista (ou atualiza) uma empresa encontrada para o job, com o seu score. */
    void registarResultado(Long jobId, Long empresaId, int score);

    void concluir(Long jobId);

    void marcarErro(Long jobId);
}
