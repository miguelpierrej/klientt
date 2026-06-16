package com.sharcky.klientt.pagamento.service;

/** Pagamentos não configurados/indisponíveis, ou plano sem preço Stripe. */
public class PagamentoIndisponivelException extends RuntimeException {

    public PagamentoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
