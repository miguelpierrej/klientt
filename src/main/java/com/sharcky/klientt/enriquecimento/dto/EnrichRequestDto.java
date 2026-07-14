package com.sharcky.klientt.enriquecimento.dto;

import java.util.List;

/**
 * Pedido enviado ao scraper ({@code POST /v1/enrich}). {@code buscaId} é o id do job (string);
 * o scraper devolve-o em cada callback para o Klientt saber que job concluir.
 */
public record EnrichRequestDto(
        String buscaId,
        List<EmpresaEnrichDto> empresas,
        String callbackUrl,
        boolean coletarEmails,
        boolean usarMaps,
        boolean verificarSmtp,
        int tamanhoLote
) {

    /** Empresa já descoberta, a enriquecer. CNPJ é o identificador principal. */
    public record EmpresaEnrichDto(
            String cnpj,
            String nome,
            String nomeFantasia,
            String municipio,
            String uf,
            String website
    ) {
    }
}
