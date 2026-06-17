package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.scraper.dto.EmpresaPayload;

import java.util.List;

/**
 * Fonte primária de descoberta de empresas (Casa dos Dados) — PLANO-DUAL-FONTE.md.
 *
 * <p>Devolve {@link EmpresaPayload} — o mesmo DTO da ingestão — para que o pipeline (ingestão +
 * enriquecimento Maps) seja o mesmo. {@code municipio} é opcional (afina a busca; {@code null} =
 * nacional). Em falha/erro devolve lista vazia (falha graciosa).
 */
public interface FonteCnpj {

    /** Busca por CNAE (nicho). */
    List<EmpresaPayload> buscarPorCnae(String cnae, String municipio, int limite);

    /** Busca textual por nome (razão social / nome fantasia). */
    List<EmpresaPayload> buscarPorNome(String nome, String municipio, int limite);
}
