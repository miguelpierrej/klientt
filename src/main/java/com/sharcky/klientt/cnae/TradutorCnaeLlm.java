package com.sharcky.klientt.cnae;

import java.util.List;

/**
 * Tradução nicho→CNAE por LLM (fallback do {@link ResolvedorCnae}).
 * Seam testável: o {@code ResolvedorCnaeImpl} depende desta interface, não do SDK.
 */
public interface TradutorCnaeLlm {

    List<Cnae> traduzir(String termo);
}
