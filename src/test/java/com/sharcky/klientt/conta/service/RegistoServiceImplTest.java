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
        // Nasce por confirmar, com token válido.
        assertThat(guardado.isEmailVerificado()).isFalse();
        assertThat(guardado.getTokenVerificacao()).isNotBlank();
        assertThat(guardado.tokenValido(java.time.LocalDateTime.now())).isTrue();
    }

    @Test
    void confirmarComTokenValidoAtivaEEsvaziaToken() {
        Utilizador u = new Utilizador();
        u.setEmailVerificado(false);
        u.setTokenVerificacao("tok123");
        u.setTokenVerificacaoExpiraEm(java.time.LocalDateTime.now().plusHours(1));
        when(utilizadorRepository.findByTokenVerificacao("tok123")).thenReturn(Optional.of(u));

        boolean ok = registoService.confirmar("tok123");

        assertThat(ok).isTrue();
        assertThat(u.isEmailVerificado()).isTrue();
        assertThat(u.getTokenVerificacao()).isNull();
        assertThat(u.getTokenVerificacaoExpiraEm()).isNull();
    }

    @Test
    void confirmarComTokenExpiradoNaoAtiva() {
        Utilizador u = new Utilizador();
        u.setEmailVerificado(false);
        u.setTokenVerificacao("tokExp");
        u.setTokenVerificacaoExpiraEm(java.time.LocalDateTime.now().minusMinutes(1));
        when(utilizadorRepository.findByTokenVerificacao("tokExp")).thenReturn(Optional.of(u));

        assertThat(registoService.confirmar("tokExp")).isFalse();
        assertThat(u.isEmailVerificado()).isFalse();
    }

    @Test
    void confirmarTokenInexistenteDevolveFalse() {
        when(utilizadorRepository.findByTokenVerificacao("nada")).thenReturn(Optional.empty());
        assertThat(registoService.confirmar("nada")).isFalse();
    }

    @Test
    void prepararReenvioSoParaContaPorConfirmar() {
        Utilizador verificado = new Utilizador();
        verificado.setEmailVerificado(true);
        when(utilizadorRepository.findByEmail("v@x.com")).thenReturn(Optional.of(verificado));
        assertThat(registoService.prepararReenvio("v@x.com")).isEmpty();

        Utilizador porConfirmar = new Utilizador();
        porConfirmar.setEmailVerificado(false);
        when(utilizadorRepository.findByEmail("p@x.com")).thenReturn(Optional.of(porConfirmar));
        assertThat(registoService.prepararReenvio("p@x.com")).isPresent();
        assertThat(porConfirmar.getTokenVerificacao()).isNotBlank();
    }

    @Test
    void emailDuplicadoLancaExcecao() {
        when(utilizadorRepository.existsByEmail("ja@existe.com")).thenReturn(true);

        assertThatThrownBy(() -> registoService.registar(new RegistoRequest("X", "ja@existe.com", "segredo123")))
                .isInstanceOf(EmailJaRegistadoException.class);

        verify(utilizadorRepository, never()).save(any());
    }
}
