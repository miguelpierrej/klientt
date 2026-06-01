package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.model.Plano;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistoServiceImpl implements RegistoService {

    private static final String PLANO_INICIAL = "Teste";

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
        planoRepository.findByNome(PLANO_INICIAL).ifPresent(u::setPlano);

        return utilizadorRepository.save(u);
    }
}
