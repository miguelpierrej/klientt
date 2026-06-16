package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.busca.dto.ResultadoBusca;

import java.util.List;

/**
 * Caso de uso de busca de leads (fluxo assíncrono — ARQUITETURA §4).
 */
public interface BuscaService {

    /** Valida a quota, cria um job do utilizador, dispara o scraper e devolve o id (para polling). */
    Long iniciar(BuscaRequest request, Long utilizadorId);

    /** Estado atual do job (do utilizador) + leads (quando concluído). Usado pelo polling. */
    ResultadoBusca consultar(Long jobId, Long utilizadorId);

    /** Leads de um job concluído, com filtros e ordenação aplicados. */
    List<LeadResponse> filtrar(Long jobId, Long utilizadorId, FiltroBusca filtro);

    /** Leads completos (para exportação), com os mesmos filtros/ordenação. */
    List<LeadDetalhe> exportar(Long jobId, Long utilizadorId, FiltroBusca filtro);
}
