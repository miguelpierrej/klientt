package com.sharcky.klientt.busca.dto;

/**
 * Lead apresentado na lista de resultados (DTO de resposta).
 * Desacoplado das entidades JPA — é isto que a camada web/Thymeleaf consome.
 */
public record LeadResponse(
        Long id,
        String nome,
        String cidade,
        double notaGoogle,
        boolean temSite,
        boolean siteLento,
        int seguidores,
        boolean proconEviteSite,
        boolean contactavel,
        int score
) {
}
