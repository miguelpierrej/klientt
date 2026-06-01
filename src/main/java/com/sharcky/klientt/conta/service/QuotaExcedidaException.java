package com.sharcky.klientt.conta.service;

/** Lançada quando o utilizador atinge o limite de leads do seu plano no mês. */
public class QuotaExcedidaException extends RuntimeException {

    public QuotaExcedidaException(int limite) {
        super("Atingiu o limite de " + limite + " leads do seu plano este mês. Faça upgrade para continuar.");
    }
}
