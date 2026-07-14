package com.sharcky.klientt.pagamento.service;

/** Stripe não configurado / pagamento indisponível. */
public class PagamentoIndisponivelException extends RuntimeException {

    public PagamentoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
