package com.sharcky.klientt.cnpj;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void sugereCidadesPorPrefixoComAcentoInsensivel() {
        List<ResolvedorMunicipio.Cidade> s = resolvedor.sugerir("bau", 8);
        assertThat(s).isNotEmpty();
        assertThat(s).anySatisfy(c -> {
            assertThat(c.nome()).isEqualTo("Bauru");
            assertThat(c.uf()).isEqualTo("SP");
        });

        // Acento/caixa não importam.
        assertThat(resolvedor.sugerir("sao paul", 8))
                .anySatisfy(c -> assertThat(c.nome()).isEqualTo("São Paulo"));
    }

    @Test
    void sugerirRespeitaLimiteEQueryCurta() {
        assertThat(resolvedor.sugerir("a", 8)).isEmpty();   // < 2 chars
        assertThat(resolvedor.sugerir(null, 8)).isEmpty();
        assertThat(resolvedor.sugerir("sa", 3)).hasSizeLessThanOrEqualTo(3);
    }
}
