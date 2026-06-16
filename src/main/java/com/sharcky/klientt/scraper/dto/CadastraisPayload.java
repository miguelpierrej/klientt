package com.sharcky.klientt.scraper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados cadastrais do CNPJ (Receita/BrasilAPI) no callback (CONTRATO-SCRAPER.md §3).
 * Tudo opcional — só preenchido quando há CNPJ e a consulta teve sucesso.
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
