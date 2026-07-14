package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;

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

    /**
     * Página de descoberta para "carregar mais": empresas + {@code cursor} de continuação
     * ({@code null}/vazio = não há mais). Usado para paginar na origem sem re-buscar.
     */
    record Pagina(List<EmpresaPayload> empresas, String cursor) {
    }

    /**
     * Busca uma página por CNAE a partir de {@code cursor} ({@code null} = início).
     * Default (fontes sem cursor): devolve a 1ª página para cursor nulo, nada para um cursor.
     */
    default Pagina buscarPaginaPorCnae(String cnae, String municipio, String cursor, int tamanho) {
        List<EmpresaPayload> empresas = cursor == null ? buscarPorCnae(cnae, municipio, tamanho) : List.of();
        return new Pagina(empresas, null);
    }
}
