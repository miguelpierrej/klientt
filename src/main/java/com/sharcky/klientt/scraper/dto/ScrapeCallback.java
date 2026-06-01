package com.sharcky.klientt.scraper.dto;

import java.util.List;

/**
 * Payload do callback do scraper com os resultados (CONTRATO-SCRAPER.md §3).
 */
public record ScrapeCallback(
        String buscaId,
        EstadoScrape estado,
        String erro,
        List<EmpresaPayload> empresas
) {
}
