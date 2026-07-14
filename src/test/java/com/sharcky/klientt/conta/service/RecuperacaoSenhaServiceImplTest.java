package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceImplTest {

    @Mock UtilizadorRepository utilizadorRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks RecuperacaoSenhaServiceImpl service;

    @Test
    void prepararGeraTokenParaContaExistente() {
        Utilizador u = new Utilizador();
        when(utilizadorRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));

        Optional<Utilizador> res = service.prepararRecuperacao("  Ana@X.com  ");

        assertThat(res).containsSame(u);
        assertThat(u.getTokenReset()).isNotBlank();
        assertThat(u.tokenResetValido(LocalDateTime.now())).isTrue();
    }

    @Test
    void prepararEmailInexistenteDevolveVazio() {
        when(utilizadorRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());
        assertThat(service.prepararRecuperacao("nao@existe.com")).isEmpty();
    }

    @Test
    void redefinirComTokenValidoAtualizaPasswordConfirmaEmailELimpaToken() {
        Utilizador u = new Utilizador();
        u.setEmailVerificado(false);
        u.setTokenReset("tok");
        u.setTokenResetExpiraEm(LocalDateTime.now().plusMinutes(30));
        when(utilizadorRepository.findByTokenReset("tok")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("novaSegura1")).thenReturn("NEWHASH");

        boolean ok = service.redefinir("tok", "novaSegura1");

        assertThat(ok).isTrue();
        assertThat(u.getPasswordHash()).isEqualTo("NEWHASH");
        assertThat(u.isEmailVerificado()).isTrue();   // clicar no link prova posse do email
        assertThat(u.getTokenReset()).isNull();
        assertThat(u.getTokenResetExpiraEm()).isNull();
    }

    @Test
    void redefinirComTokenExpiradoNaoAltera() {
        Utilizador u = new Utilizador();
        u.setTokenReset("tokExp");
        u.setTokenResetExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(utilizadorRepository.findByTokenReset("tokExp")).thenReturn(Optional.of(u));

        assertThat(service.redefinir("tokExp", "novaSegura1")).isFalse();
        assertThat(u.getPasswordHash()).isNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void redefinirTokenInexistenteDevolveFalse() {
        when(utilizadorRepository.findByTokenReset("nada")).thenReturn(Optional.empty());
        assertThat(service.redefinir("nada", "novaSegura1")).isFalse();
    }

    @Test
    void tokenValidoRefleteEstadoDoToken() {
        Utilizador valido = new Utilizador();
        valido.setTokenReset("v");
        valido.setTokenResetExpiraEm(LocalDateTime.now().plusMinutes(10));
        when(utilizadorRepository.findByTokenReset("v")).thenReturn(Optional.of(valido));
        assertThat(service.tokenValido("v")).isTrue();

        assertThat(service.tokenValido("  ")).isFalse();
    }
}
