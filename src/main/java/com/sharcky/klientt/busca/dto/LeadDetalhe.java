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
        String website,
        String fonte,
        boolean contactavel,
        List<ContatoView> contatos,
        LocalDateTime atualizadoEm,
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
