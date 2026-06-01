package com.sharcky.klientt.conta.service;

/** Lançada quando se tenta registar um email já existente. */
public class EmailJaRegistadoException extends RuntimeException {

    public EmailJaRegistadoException(String email) {
        super("Já existe uma conta com o email " + email);
    }
}
