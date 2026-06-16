package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.scraper.dto.EmpresaPayload;

import java.util.List;

/**
 * Fonte de descoberta de empresas por CNAE + região (PLANO-DUAL-FONTE.md, Fase D).
 *
 * <p>Devolve {@link EmpresaPayload} — o mesmo DTO do contrato do scraper — para que a
 * ingestão ({@code IngestaoService}) seja exatamente a mesma das duas fontes.
 */
public interface FonteCnpj {

    /**
     * Busca empresas ativas com o CNAE indicado no município. Devolve lista vazia quando
     * a fonte está desligada ou em caso de erro (falha graciosa).
     */
    List<EmpresaPayload> buscarPorCnae(String cnae, String municipio, int limite);
}
