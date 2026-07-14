package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecuperacaoSenhaServiceImpl implements RecuperacaoSenhaService {

    /** Validade do link de redefinição (curta, por segurança). */
    private static final long HORAS_VALIDADE_TOKEN = 1;

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;

    public RecuperacaoSenhaServiceImpl(UtilizadorRepository utilizadorRepository, PasswordEncoder passwordEncoder) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Optional<Utilizador> prepararRecuperacao(String email) {
        return utilizadorRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> {
                    u.setTokenReset(UUID.randomUUID().toString().replace("-", ""));
                    u.setTokenResetExpiraEm(LocalDateTime.now().plusHours(HORAS_VALIDADE_TOKEN));
                    return u;   // dirty checking persiste; o controlador envia o email
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return token != null && !token.isBlank()
                && utilizadorRepository.findByTokenReset(token)
                .map(u -> u.tokenResetValido(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean redefinir(String token, String novaPassword) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return utilizadorRepository.findByTokenReset(token)
                .filter(u -> u.tokenResetValido(LocalDateTime.now()))
                .map(u -> {
                    u.setPasswordHash(passwordEncoder.encode(novaPassword));
                    // Clicar no link prova posse do email → aproveita para confirmar a conta.
                    u.setEmailVerificado(true);
                    u.setTokenReset(null);
                    u.setTokenResetExpiraEm(null);
                    return true;
                })
                .orElse(false);
    }
}
