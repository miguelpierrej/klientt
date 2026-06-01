package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.conta.model.Plano;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

    @Mock
    UtilizadorRepository utilizadorRepository;
    @Mock
    JobResultadoRepository jobResultadoRepository;
    @InjectMocks
    QuotaServiceImpl quotaService;

    @Test
    void abaixoDoLimitePermite() {
        comUtilizadorComPlano(1L, 20);
        when(jobResultadoRepository.contarLeadsDoUtilizadorDesde(eq(1L), any(LocalDateTime.class))).thenReturn(10L);

        assertThatCode(() -> quotaService.garantirDisponibilidade(1L)).doesNotThrowAnyException();
    }

    @Test
    void noLimiteBloqueia() {
        comUtilizadorComPlano(1L, 20);
        when(jobResultadoRepository.contarLeadsDoUtilizadorDesde(eq(1L), any(LocalDateTime.class))).thenReturn(20L);

        assertThatThrownBy(() -> quotaService.garantirDisponibilidade(1L))
                .isInstanceOf(QuotaExcedidaException.class);
    }

    @Test
    void semPlanoBloqueia() {
        Utilizador u = new Utilizador();
        u.setPlano(null);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(u));
        when(jobResultadoRepository.contarLeadsDoUtilizadorDesde(eq(1L), any(LocalDateTime.class))).thenReturn(0L);

        assertThatThrownBy(() -> quotaService.garantirDisponibilidade(1L))
                .isInstanceOf(QuotaExcedidaException.class);
    }

    @Test
    void consumoMesAtualDevolveContagem() {
        when(jobResultadoRepository.contarLeadsDoUtilizadorDesde(eq(1L), any(LocalDateTime.class))).thenReturn(7L);
        assertThat(quotaService.consumoMesAtual(1L)).isEqualTo(7L);
    }

    private void comUtilizadorComPlano(Long id, int limite) {
        Plano p = new Plano();
        p.setLimiteLeadsMes(limite);
        Utilizador u = new Utilizador();
        u.setPlano(p);
        when(utilizadorRepository.findById(id)).thenReturn(Optional.of(u));
    }
}
