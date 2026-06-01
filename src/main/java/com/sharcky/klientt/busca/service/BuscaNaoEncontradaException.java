package com.sharcky.klientt.busca.service;

/**
 * Lançada quando um job não existe ou não pertence ao utilizador autenticado.
 * Não distingue os dois casos (não revela a existência de jobs de outros).
 */
public class BuscaNaoEncontradaException extends RuntimeException {

    public BuscaNaoEncontradaException(Long jobId) {
        super("Busca não encontrada: " + jobId);
    }
}
