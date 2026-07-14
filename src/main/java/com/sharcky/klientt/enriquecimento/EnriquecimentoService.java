package com.sharcky.klientt.enriquecimento;

import com.sharcky.klientt.enriquecimento.dto.EnrichCallback;

/** Aplica um callback do scraper: funde as empresas enriquecidas na cache e conclui o job. */
public interface EnriquecimentoService {

    void aplicar(EnrichCallback callback);
}
