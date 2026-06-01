package com.sharcky.klientt.scraper.client;

import com.sharcky.klientt.scraper.dto.ScrapeAck;
import com.sharcky.klientt.scraper.dto.ScrapeRequest;

/**
 * Cliente para o serviço de scraping (Klientt → scraper, CONTRATO-SCRAPER.md §2).
 * O scraper responde de forma assíncrona via callback (webhook).
 */
public interface ScraperClient {

    /** Header do segredo partilhado usado nos dois sentidos. */
    String TOKEN_HEADER = "X-Klientt-Token";

    /** Inicia uma busca no scraper. Devolve o ACEITE (202). */
    ScrapeAck iniciarBusca(ScrapeRequest request);
}
