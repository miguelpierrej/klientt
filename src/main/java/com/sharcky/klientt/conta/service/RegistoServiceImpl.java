package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistoServiceImpl implements RegistoService {

    private static final String PLANO_INICIAL = "Teste";
    /** Validade do link de confirmação. */
    private static final long HORAS_VALIDADE_TOKEN = 24;

    private final UtilizadorRepository utilizadorRepository;
    private final PlanoRepository planoRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistoServiceImpl(UtilizadorRepository utilizadorRepository, PlanoRepository planoRepository,
                              PasswordEncoder passwordEncoder) {
        this.utilizadorRepository = utilizadorRepository;
        this.planoRepository = planoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Utilizador registar(RegistoRequest request) {
        String email = request.email().trim().toLowerCase();
        if (utilizadorRepository.existsByEmail(email)) {
            throw new EmailJaRegistadoException(email);
        }

        Utilizador u = new Utilizador();
        u.setNome(request.nome().trim());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setEmailVerificado(false);
        atribuirNovoToken(u);
        planoRepository.findByNome(PLANO_INICIAL).ifPresent(u::setPlano);

        return utilizadorRepository.save(u);
    }

    @Override
    @Transactional
    public Optional<Utilizador> prepararReenvio(String email) {
        return utilizadorRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> !u.isEmailVerificado())
                .map(u -> {
                    atribuirNovoToken(u);
                    return u;   // dirty checking persiste; o controlador envia o email
                });
    }

    @Override
    @Transactional
    public boolean confirmar(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return utilizadorRepository.findByTokenVerificacao(token)
                .filter(u -> u.tokenValido(LocalDateTime.now()))
                .map(u -> {
                    u.setEmailVerificado(true);
                    u.setTokenVerificacao(null);
                    u.setTokenVerificacaoExpiraEm(null);
                    return true;
                })
                .orElse(false);
    }

    private void atribuirNovoToken(Utilizador u) {
        u.setTokenVerificacao(UUID.randomUUID().toString().replace("-", ""));
        u.setTokenVerificacaoExpiraEm(LocalDateTime.now().plusHours(HORAS_VALIDADE_TOKEN));
    }
}
