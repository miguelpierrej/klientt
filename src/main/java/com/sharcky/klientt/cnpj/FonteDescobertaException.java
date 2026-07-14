package com.sharcky.klientt.cnpj;

/**
 * Falha real ao contactar uma fonte de descoberta (fora do ar, timeout, erro HTTP) — distinta de
 * "sem resultados". Sinaliza ao {@link FonteDescobertaRouter} que deve tentar o fallback.
 */
public class FonteDescobertaException extends RuntimeException {

    public FonteDescobertaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
