package com.sharcky.klientt.scraper.dto;

import com.sharcky.klientt.busca.dto.TipoBusca;

/**
 * Pedido enviado pelo Klientt ao scraper (CONTRATO-SCRAPER.md §2).
 */
public record ScrapeRequest(
        String buscaId,
        TipoBusca tipo,
        String termo,
        String regiao,
        int limite,
        String callbackUrl
) {
}
