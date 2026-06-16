package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Empresa;

/**
 * Cache de empresas (ARQUITETURA §3): guarda/atualiza empresas para reutilização
 * entre buscas, evitando recoletar. Identidade por CNPJ; fallback nome + cidade.
 */
public interface EmpresaCacheService {

    /**
     * Insere a empresa, ou funde os dados na existente (mesmo CNPJ, ou nome+cidade quando não há
     * CNPJ). O merge é não-destrutivo: um valor nulo não apaga o que já existe. Devolve a persistida.
     */
    Empresa upsert(Empresa fresca);
}
