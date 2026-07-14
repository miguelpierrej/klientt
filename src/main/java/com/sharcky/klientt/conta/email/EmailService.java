package com.sharcky.klientt.conta.email;

/** Envio de emails transacionais. */
public interface EmailService {

    /** Envia o email de confirmação de conta com o {@code linkVerificacao}. Best-effort (não lança). */
    void enviarVerificacao(String para, String nome, String linkVerificacao);
}
