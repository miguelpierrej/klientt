package com.sharcky.klientt.cnae;

import java.util.List;

/**
 * Resolve o que o utilizador escreve (ex.: "barbearias") para código(s) CNAE,
 * usados na busca por CNAE da {@code FonteCnpj} (PLANO-DUAL-FONTE.md, Fase D).
 *
 * <p>Estratégia: tabela determinística para os nichos comuns (rápido/grátis) e,
 * no que falhar, fallback para um LLM (Claude). Devolve vazio se não resolver.
 */
public interface ResolvedorCnae {

    /** Melhor CNAE para o termo (máx. 1) — usado como fallback automático na descoberta. */
    List<Cnae> resolver(String termo);

    /**
     * Lista ordenada de CNAEs candidatos para o termo (para o utilizador confirmar antes de buscar).
     * Combina sinónimos, sugestão do LLM e correspondências do catálogo; vazia se nada bater.
     */
    List<Cnae> candidatos(String termo);
}
