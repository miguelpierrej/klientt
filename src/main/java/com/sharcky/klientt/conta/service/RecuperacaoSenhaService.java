package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.model.Utilizador;

import java.util.Optional;

/**
 * Recuperação de password: pedir link por email e redefinir com token de uso único.
 */
public interface RecuperacaoSenhaService {

    /**
     * Gera um token de redefinição para o email (se existir uma conta). Devolve o utilizador
     * (para o controlador enviar o email) ou vazio se o email não existir.
     */
    Optional<Utilizador> prepararRecuperacao(String email);

    /** Verifica se o token é válido (para mostrar o formulário de nova password). */
    boolean tokenValido(String token);

    /** Redefine a password a partir do token (uso único, com validade). Devolve true se redefiniu. */
    boolean redefinir(String token, String novaPassword);
}
