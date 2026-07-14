package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditosServiceImplTest {

    @Mock UtilizadorRepository utilizadorRepository;
    @Mock JobResultadoRepository jobResultadoRepository;

    /** Página grátis = 20. */
    private CreditosServiceImpl service() {
        return new CreditosServiceImpl(utilizadorRepository, jobResultadoRepository, 20);
    }

    private void comCreditos(int creditos) {
        Utilizador u = new Utilizador();
        u.setCreditosLeads(creditos);
        lenient().when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(u));
    }

    @Test
    void consumoIgnoraAPrimeiraPaginaDeCadaBusca() {
        // jobs com 20, 40 e 5 leads → grátis os primeiros 20 de cada → 0 + 20 + 0 = 20.
        when(jobResultadoRepository.contarLeadsPorJobDoUtilizador(1L)).thenReturn(List.of(20L, 40L, 5L));
        assertThat(service().consumido(1L)).isEqualTo(20L);
    }

    @Test
    void disponivelEhCompradoMenosConsumido() {
        comCreditos(3000);
        when(jobResultadoRepository.contarLeadsPorJobDoUtilizador(1L)).thenReturn(List.of(20L, 40L)); // consumido=20
        assertThat(service().disponivel(1L)).isEqualTo(2980L);
        assertThat(service().temDisponivel(1L)).isTrue();
    }

    @Test
    void semComprasNaoTemDisponivel() {
        comCreditos(0);
        when(jobResultadoRepository.contarLeadsPorJobDoUtilizador(1L)).thenReturn(List.of(20L)); // 1ª página grátis
        assertThat(service().disponivel(1L)).isZero();
        assertThat(service().temDisponivel(1L)).isFalse();
    }

    @Test
    void creditarSomaAoSaldo() {
        Utilizador u = new Utilizador();
        u.setCreditosLeads(100);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(u));

        service().creditar(1L, 3000);

        assertThat(u.getCreditosLeads()).isEqualTo(3100);   // acumula
    }
}
