package com.sharcky.klientt.cnpj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedorMunicipioTest {

    private final ResolvedorMunicipio resolvedor = new ResolvedorMunicipio();

    @Test
    void resolveCidadeComUf() {
        assertThat(resolvedor.codigo("Bauru", "SP")).contains("3506003");
        assertThat(resolvedor.codigo("São Paulo", "SP")).contains("3550308");
        assertThat(resolvedor.codigo("Campinas", "SP")).contains("3509502");
    }

    @Test
    void ignoraCaixaEAcentos() {
        assertThat(resolvedor.codigo("sao paulo", "sp")).contains("3550308");
        assertThat(resolvedor.codigo("  BAURU ", "Sp")).contains("3506003");
    }

    @Test
    void cidadeInexistenteDevolveVazio() {
        assertThat(resolvedor.codigo("Cidadeinexistente", "SP")).isEmpty();
        assertThat(resolvedor.codigo(null, "SP")).isEmpty();
    }

    @Test
    void semUfSoResolveNomesUnicos() {
        // "São Paulo" (município) existe em >1 UF? Não — mas há nomes repetidos entre estados.
        // Bauru é único no país → resolve sem UF.
        assertThat(resolvedor.codigo("Bauru", null)).contains("3506003");
        // "Bom Jesus" existe em vários estados → ambíguo sem UF.
        assertThat(resolvedor.codigo("Bom Jesus", null)).isEmpty();
    }
}
