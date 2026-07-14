package com.sharcky.klientt.cnpj;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Cadeia de enriquecimento de contacto por CNPJ (ver 'Nova integração.md', D2):
 * <b>CNPJá open (primário) → BrasilAPI (fallback)</b>. Só consulta a BrasilAPI se o CNPJá
 * não devolveu nada. É o bean {@code @Primary} de {@link FonteContatoCnpj}.
 */
@Service
@Primary
public class CadeiaContatoFonte implements FonteContatoCnpj {

    private final CnpjaContatoFonte cnpja;
    private final BrasilApiContatoFonte brasilApi;

    public CadeiaContatoFonte(CnpjaContatoFonte cnpja, BrasilApiContatoFonte brasilApi) {
        this.cnpja = cnpja;
        this.brasilApi = brasilApi;
    }

    @Override
    public Contatos consultar(String cnpj) {
        Contatos c = cnpja.consultar(cnpj);
        return c.isVazio() ? brasilApi.consultar(cnpj) : c;
    }
}
