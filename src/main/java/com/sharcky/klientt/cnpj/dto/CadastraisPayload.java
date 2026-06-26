package com.sharcky.klientt.cnpj.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados cadastrais do CNPJ (Receita / Casa dos Dados). Tudo opcional.
 */
public record CadastraisPayload(
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
