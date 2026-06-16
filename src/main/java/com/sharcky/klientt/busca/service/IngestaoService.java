package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.scraper.dto.EmpresaPayload;

import java.util.List;

/**
 * Ingestão partilhada de empresas vindas de uma fonte de descoberta.
 *
 * <p>Concentra num só lugar o caminho comum — cache (upsert), cálculo do score e ligação ao job —
 * para que tanto o callback do scraper (Google Maps) como, no futuro, a fonte CNPJ-por-CNAE
 * (ver PLANO-DUAL-FONTE.md) usem exatamente o mesmo merge e pontuação.
 */
public interface IngestaoService {

    /**
     * Faz upsert das empresas em cache e, quando {@code jobId} não é {@code null}, calcula o score
     * e liga cada empresa ao job. Com {@code jobId} {@code null} apenas faz cache (sem ligar ao job).
     *
     * @param empresas lista de empresas a ingerir (tolera {@code null} / vazia)
     * @param jobId    id do job de busca, ou {@code null} para apenas alimentar a cache
     */
    void ingerir(List<EmpresaPayload> empresas, Long jobId);
}
