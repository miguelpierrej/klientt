package com.sharcky.klientt.busca.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Informação completa de um lead, para a vista de detalhe (modal).
 */
public record LeadDetalhe(
        Long id,
        String nome,
        String cidade,
        String cnpj,
        String telefone,
        String endereco,
        String website,
        String fonte,
        LocalDateTime atualizadoEm,
        int score,
        // sinais
        BigDecimal notaGoogle,
        Integer numReviews,
        Boolean siteExiste,
        Integer siteVelocidadeMs,
        Boolean siteHttps,
        Integer siteNumPaginas,
        String siteReputacao,
        boolean proconEviteSite,
        List<RedeView> redes
) {
}
