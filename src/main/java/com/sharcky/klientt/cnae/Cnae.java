package com.sharcky.klientt.cnae;

/**
 * Código CNAE (Classificação Nacional de Atividades Econômicas) + descrição.
 * Resultado da resolução nicho→CNAE (PLANO-DUAL-FONTE.md, Fase D).
 */
public record Cnae(String codigo, String descricao) {
}
