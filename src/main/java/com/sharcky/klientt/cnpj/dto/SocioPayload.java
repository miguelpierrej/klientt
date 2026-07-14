package com.sharcky.klientt.cnpj.dto;

import java.time.LocalDate;

/**
 * Sócio de uma empresa (Quadro de Sócios e Administradores — QSA da Receita).
 */
public record SocioPayload(
        String nome,
        String qualificacao,
        String faixaEtaria,
        LocalDate desde
) {
}
