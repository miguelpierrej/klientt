package com.sharcky.klientt.busca.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SugestaoCnaeTest {

    @Test
    void formataCodigoDe7Digitos() {
        SugestaoCnae s = SugestaoCnae.de("5611201", "Restaurantes e similares");
        assertThat(s.codigo()).isEqualTo("5611201");           // vai no campo oculto (só dígitos)
        assertThat(s.codigoFormatado()).isEqualTo("5611-2/01"); // exibição
        assertThat(s.descricao()).isEqualTo("Restaurantes e similares");
    }

    @Test
    void codigoInesperadoNaoQuebra() {
        assertThat(SugestaoCnae.de("123", "x").codigoFormatado()).isEqualTo("123");
        assertThat(SugestaoCnae.de(null, "x").codigoFormatado()).isNull();
    }
}
