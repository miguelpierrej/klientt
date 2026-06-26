package com.sharcky.klientt.cnpj.dto;

import java.util.List;

/**
 * Uma empresa devolvida pela fonte de descoberta (Casa dos Dados). Carrega os cadastrais e
 * <b>todos</b> os contactos da Receita ({@code telefones}/{@code emails}); {@code telefone}/{@code email}
 * são o primeiro de cada (conveniência para exibição). É o DTO de entrada da ingestão.
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
        CadastraisPayload cadastrais,
        List<String> telefones,
        List<String> emails
) {
}
