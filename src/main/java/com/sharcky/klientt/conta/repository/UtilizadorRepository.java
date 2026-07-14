package com.sharcky.klientt.conta.repository;

import com.sharcky.klientt.conta.model.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    Optional<Utilizador> findByEmail(String email);

    Optional<Utilizador> findByTokenVerificacao(String tokenVerificacao);

    Optional<Utilizador> findByTokenReset(String tokenReset);

    boolean existsByEmail(String email);
}
