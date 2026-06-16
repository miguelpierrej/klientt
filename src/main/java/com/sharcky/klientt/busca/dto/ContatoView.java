package com.sharcky.klientt.busca.dto;

/**
 * Meio de contacto de um lead, para apresentação (detalhe / export).
 */
public record ContatoView(
        String tipo,
        String valor,
        String fonte,
        boolean principal,
        boolean verificado
) {
}
