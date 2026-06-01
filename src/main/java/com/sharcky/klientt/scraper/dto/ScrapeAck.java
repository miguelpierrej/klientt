package com.sharcky.klientt.scraper.dto;

/**
 * Resposta do scraper ao pedido inicial (202 Accepted) — CONTRATO-SCRAPER.md §2.
 */
public record ScrapeAck(
        String scrapeId,
        String buscaId,
        EstadoScrape estado
) {
}
