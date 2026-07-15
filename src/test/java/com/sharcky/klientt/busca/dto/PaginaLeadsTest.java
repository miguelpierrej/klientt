package com.sharcky.klientt.busca.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaLeadsTest {

    private static List<LeadResponse> leads(int n) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(i -> new LeadResponse((long) i, "E" + i, "SP", null, null, null, null, true, null, null))
                .toList();
    }

    @Test
    void primeiraPaginaDeVinteEmVinteCinco() {
        PaginaLeads p = PaginaLeads.de(leads(25), 1, 20);
        assertThat(p.leads()).hasSize(20);
        assertThat(p.pagina()).isEqualTo(1);
        assertThat(p.totalPaginas()).isEqualTo(2);
        assertThat(p.total()).isEqualTo(25);
        assertThat(p.temAnterior()).isFalse();
        assertThat(p.temSeguinte()).isTrue();
    }

    @Test
    void segundaPaginaTemOResto() {
        PaginaLeads p = PaginaLeads.de(leads(25), 2, 20);
        assertThat(p.leads()).hasSize(5);
        assertThat(p.leads().get(0).id()).isEqualTo(21L);
        assertThat(p.temSeguinte()).isFalse();
        assertThat(p.temAnterior()).isTrue();
    }

    @Test
    void paginaForaDoIntervaloEhLimitada() {
        PaginaLeads p = PaginaLeads.de(leads(25), 99, 20);
        assertThat(p.pagina()).isEqualTo(2);          // clamped ao máximo
        assertThat(p.leads()).hasSize(5);
    }

    @Test
    void listaVaziaTemUmaPaginaSemLeads() {
        PaginaLeads p = PaginaLeads.de(List.of(), 1, 20);
        assertThat(p.total()).isZero();
        assertThat(p.totalPaginas()).isEqualTo(1);
        assertThat(p.leads()).isEmpty();
        assertThat(p.temSeguinte()).isFalse();
    }
}
