package com.sharcky.klientt.scraper.dto;

/**
 * Perfil de rede social no callback do scraper (CONTRATO-SCRAPER.md §3).
 */
public record RedePayload(
        String rede,
        String url,
        Integer seguidores
) {
}
