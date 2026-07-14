package com.sharcky.klientt.busca.dto;

import java.time.LocalDate;

/** Sócio de um lead (QSA), para exibição no detalhe. */
public record SocioView(
        String nome,
        String qualificacao,
        String faixaEtaria,
        LocalDate desde
) {
}
