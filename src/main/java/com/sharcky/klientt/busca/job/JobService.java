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

    /**
     * Marca que uma das fontes (scraper ou CNPJ-por-CNAE) terminou. Quando todas as fontes
     * esperadas reportarem, conclui o job. Dual-fonte (PLANO-DUAL-FONTE.md, Fase E).
     */
    void marcarFonteConcluida(Long jobId);

    void marcarErro(Long jobId);
}
