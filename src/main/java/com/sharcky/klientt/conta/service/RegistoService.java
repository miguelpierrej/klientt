package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.model.Utilizador;

import java.util.Optional;

/**
 * Registo de novos utilizadores + confirmação de email em dois passos.
 */
public interface RegistoService {

    /**
     * Cria um utilizador (por confirmar) no plano de teste, com token de verificação gerado.
     * Lança {@link EmailJaRegistadoException} se o email existir. O envio do email é do controlador.
     */
    Utilizador registar(RegistoRequest request);

    /**
     * Regenera o token de um utilizador ainda não confirmado, para reenviar o email.
     * Devolve vazio se o email não existir ou já estiver confirmado.
     */
    Optional<Utilizador> prepararReenvio(String email);

    /**
     * Confirma o email a partir do token (uso único, com validade). Devolve true se confirmou.
     */
    boolean confirmar(String token);
}
