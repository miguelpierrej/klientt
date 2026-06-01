package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Empresa;

/**
 * Cache de empresas (ARQUITETURA §3): guarda/atualiza empresas para reutilização
 * entre buscas, evitando recoletar. Upsert por nome + cidade.
 */
public interface EmpresaCacheService {

    /** Insere a empresa, ou atualiza a existente (mesmo nome+cidade). Devolve a persistida. */
    Empresa upsert(Empresa fresca);
}
