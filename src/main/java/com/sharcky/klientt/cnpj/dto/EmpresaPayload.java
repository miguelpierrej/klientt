package com.sharcky.klientt.cnpj.dto;

/**
 * Uma empresa devolvida pela fonte de descoberta (Casa dos Dados). Carrega os cadastrais e os
 * contactos da Receita; é o DTO de entrada da ingestão.
 */
public record EmpresaPayload(
        String nome,
        String cnpj,
        String telefone,
        String email,
        String endereco,
        String cidade,
        String website,
        Double lat,
        Double lng,
        String fonte,
        CadastraisPayload cadastrais
) {
}
