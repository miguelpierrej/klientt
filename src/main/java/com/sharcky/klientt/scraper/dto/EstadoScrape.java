package com.sharcky.klientt.scraper.dto;

/**
 * Estado de um scrape no contrato com o serviço de scraping (CONTRATO-SCRAPER.md §4).
 */
public enum EstadoScrape {
    ACEITE,
    PARCIAL,
    CONCLUIDO,
    ERRO
}
