package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;

import java.util.List;

/**
 * Ingestão partilhada de empresas vindas da fonte de descoberta (Casa dos Dados).
 *
 * <p>Concentra num só lugar o caminho comum — cache (upsert) e ligação ao job.
 */
public interface IngestaoService {

    /**
     * Faz upsert das empresas em cache e, quando {@code jobId} não é {@code null}, liga cada empresa
     * ao job. Com {@code jobId} {@code null} apenas faz cache (sem ligar ao job).
     *
     * @param empresas lista de empresas a ingerir (tolera {@code null} / vazia)
     * @param jobId    id do job de busca, ou {@code null} para apenas alimentar a cache
     */
    void ingerir(List<EmpresaPayload> empresas, Long jobId);
}
