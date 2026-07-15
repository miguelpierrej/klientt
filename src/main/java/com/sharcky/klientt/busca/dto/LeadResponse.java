package com.sharcky.klientt.busca.dto;

/**
 * Lead apresentado na lista de resultados (DTO de resposta).
 * Desacoplado das entidades JPA — é isto que a camada web/Thymeleaf consome.
 * {@code fit} é o rótulo de relevância ao perfil do utilizador ("Ótimo fit"/"Bom fit"/null).
 */
public record LeadResponse(
        Long id,
        String nome,
        String cidade,
        String cnpj,
        String telefone,
        String email,
        String porte,
        boolean contactavel,
        Double nota,
        String fit
) {
    /** Cópia com o rótulo de fit preenchido (computado por request, fora do mapper). */
    public LeadResponse comFit(String fit) {
        return new LeadResponse(id, nome, cidade, cnpj, telefone, email, porte, contactavel, nota, fit);
    }
}
