package com.sharcky.klientt.busca.dto;

/**
 * Filtros e ordenação aplicados à lista de leads de um job já concluído.
 */
public record FiltroBusca(
        OrdenarPor ordenar,
        boolean comContato
) {
    public OrdenarPor ordenarOuPadrao() {
        return ordenar != null ? ordenar : OrdenarPor.RELEVANCIA;
    }
}
