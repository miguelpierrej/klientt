package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.model.Plano;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistoServiceImplTest {

    @Mock
    UtilizadorRepository utilizadorRepository;
    @Mock
    PlanoRepository planoRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    RegistoServiceImpl registoService;

    @Test
    void registaNoPlanoTesteComPasswordCifrada() {
        when(utilizadorRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(passwordEncoder.encode("segredo123")).thenReturn("HASH");
        Plano teste = new Plano();
        teste.setNome("Teste");
        when(planoRepository.findByNome("Teste")).thenReturn(Optional.of(teste));
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(inv -> inv.getArgument(0));

        // email com maiúsculas e espaços → deve ser normalizado
        registoService.registar(new RegistoRequest("Ana", "  Ana@X.com  ", "segredo123"));

        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(captor.capture());
        Utilizador guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo("ana@x.com");
        assertThat(guardado.getPasswordHash()).isEqualTo("HASH");
        assertThat(guardado.getPlano()).isSameAs(teste);
        assertThat(guardado.getNome()).isEqualTo("Ana");
    }

    @Test
    void emailDuplicadoLancaExcecao() {
        when(utilizadorRepository.existsByEmail("ja@existe.com")).thenReturn(true);

        assertThatThrownBy(() -> registoService.registar(new RegistoRequest("X", "ja@existe.com", "segredo123")))
                .isInstanceOf(EmailJaRegistadoException.class);

        verify(utilizadorRepository, never()).save(any());
    }
}
