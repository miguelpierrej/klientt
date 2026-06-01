package com.sharcky.klientt.scraper.ingest;

import com.sharcky.klientt.scraper.dto.ScrapeCallback;

/**
 * Port: trata os resultados recebidos do scraper no callback.
 * Implementado pela feature de busca (que liga os resultados ao job).
 * Mantém o webhook do scraper sem dependência de compilação da feature busca.
 */
public interface ScrapeCallbackHandler {

    void tratar(ScrapeCallback callback);
}
