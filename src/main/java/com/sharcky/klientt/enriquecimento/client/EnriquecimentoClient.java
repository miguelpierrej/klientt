package com.sharcky.klientt.enriquecimento.client;

import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoRequest;

/**
 * Cliente do enriquecimento Maps (Klientt → scraper). O scraper responde de forma assíncrona
 * via callback (por empresa/CNPJ). PLANO-DUAL-FONTE.md, Fase 2.
 */
public interface EnriquecimentoClient {

    void enriquecer(EnriquecimentoRequest request);
}
