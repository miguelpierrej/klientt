package com.sharcky.klientt.busca.dto;

/**
 * Filtros e ordenação aplicados à lista de leads de um job já concluído.
 * Os flags são "incluir apenas os que..."; combinam-se com E (AND).
 */
public record FiltroBusca(
        OrdenarPor ordenar,
        boolean semSite,
        boolean notaBaixa,
        boolean poucosSeguidores,
        boolean procon
) {
    public OrdenarPor ordenarOuPadrao() {
        return ordenar != null ? ordenar : OrdenarPor.SCORE;
    }
}
