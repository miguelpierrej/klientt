package com.sharcky.klientt.scraper.dto;

import java.math.BigDecimal;

/**
 * Sinais enriquecidos de uma empresa no callback (CONTRATO-SCRAPER.md §3).
 * Reclame Aqui fora do MVP — não incluído.
 */
public record SinaisPayload(
        BigDecimal notaGoogle,
        Integer numReviews,
        Boolean siteExiste,
        Integer siteVelocidadeMs,
        Boolean siteHttps,
        Integer siteNumPaginas,
        String siteReputacao,
        boolean proconEviteSite
) {
}
