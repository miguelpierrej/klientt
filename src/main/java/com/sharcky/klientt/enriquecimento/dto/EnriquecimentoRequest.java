package com.sharcky.klientt.enriquecimento.dto;

/**
 * Pedido de enriquecimento Maps de uma empresa (Klientt → scraper) — PLANO-DUAL-FONTE.md, Fase 2.
 * O scraper procura no Maps por {@code nome} (em {@code municipio}), confirma o endereço e devolve
 * os sinais, no callback, atados a este {@code cnpj}.
 */
public record EnriquecimentoRequest(
        String buscaId,
        String cnpj,
        String nome,
        String municipio,
        String endereco,
        String callbackUrl
) {
}
