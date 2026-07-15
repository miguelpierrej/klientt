package com.sharcky.klientt.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock PerfilClienteRepository repository;
    @Mock com.sharcky.klientt.cnae.CnaeCatalogoRepository cnaeRepo;
    @InjectMocks PerfilService service;

    @Test
    void salvarNormalizaEConclui() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.salvar(1L, new PerfilForm("sites", " 5611201 , 5611203 ,", "Bauru/SP",
                List.of("MEI", "MICRO"), true, false, true));

        ArgumentCaptor<PerfilCliente> cap = ArgumentCaptor.forClass(PerfilCliente.class);
        verify(repository).save(cap.capture());
        PerfilCliente p = cap.getValue();
        assertThat(p.getUtilizadorId()).isEqualTo(1L);
        assertThat(p.isConcluido()).isTrue();
        assertThat(p.getOferta()).isEqualTo("sites");
        assertThat(p.getNichosAlvo()).isEqualTo("5611201,5611203");   // trim + remove vazios
        assertThat(p.getPortesAlvo()).isEqualTo("MEI,MICRO");
        assertThat(p.isQuerSemSite()).isTrue();
        assertThat(p.nichos()).containsExactly("5611201", "5611203");
        assertThat(p.temAlvo()).isTrue();
    }

    @Test
    void pularConcluiSemAlvo() {
        when(repository.findById(2L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.pular(2L);

        ArgumentCaptor<PerfilCliente> cap = ArgumentCaptor.forClass(PerfilCliente.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().isConcluido()).isTrue();
        assertThat(cap.getValue().temAlvo()).isFalse();
    }

    @Test
    void concluidoRefleteEstado() {
        PerfilCliente feito = new PerfilCliente();
        feito.setConcluido(true);
        when(repository.findById(3L)).thenReturn(Optional.of(feito));
        assertThat(service.concluido(3L)).isTrue();

        when(repository.findById(4L)).thenReturn(Optional.empty());
        assertThat(service.concluido(4L)).isFalse();   // sem perfil → onboarding pendente
    }
}
