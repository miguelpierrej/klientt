package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.model.Utilizador;

/**
 * Registo de novos utilizadores.
 */
public interface RegistoService {

    /** Cria um utilizador no plano de teste. Lança {@link EmailJaRegistadoException} se o email existir. */
    Utilizador registar(RegistoRequest request);
}
