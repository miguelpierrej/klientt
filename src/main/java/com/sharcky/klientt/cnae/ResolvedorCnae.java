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

    List<Cnae> resolver(String termo);
}
