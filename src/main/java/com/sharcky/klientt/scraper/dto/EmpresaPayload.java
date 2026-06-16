package com.sharcky.klientt.scraper.dto;

import java.util.List;

/**
 * Uma empresa devolvida pelo scraper no callback (CONTRATO-SCRAPER.md §3).
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
        SinaisPayload sinais,
        List<RedePayload> redes,
        CadastraisPayload cadastrais
) {
}
