package com.sharcky.klientt.busca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        String email,
        String endereco,
        String enderecoMaps,
        Boolean enderecoDivergente,
        String website,
        String fonte,
        boolean contactavel,
        List<ContatoView> contatos,
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
        List<RedeView> redes,
        // dados cadastrais (CNPJ)
        String razaoSocial,
        String nomeFantasia,
        String situacaoCadastral,
        LocalDate dataAbertura,
        BigDecimal capitalSocial,
        String porte,
        String naturezaJuridica,
        String cnaePrincipal,
        Boolean optanteSimples,
        Boolean optanteMei
) {
}
