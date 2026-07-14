package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.FonteContatoCnpj.Contatos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadeiaContatoFonteTest {

    @Mock CnpjaContatoFonte cnpja;
    @Mock BrasilApiContatoFonte brasilApi;

    @Test
    void usaCnpjaEQuandoTemContactoNaoChamaOFallback() {
        Contatos doCnpja = new Contatos(List.of("1637112002"), List.of("a@b.com"));
        when(cnpja.consultar("1")).thenReturn(doCnpja);

        Contatos r = new CadeiaContatoFonte(cnpja, brasilApi).consultar("1");

        assertThat(r).isEqualTo(doCnpja);
        verify(brasilApi, never()).consultar(any());
    }

    @Test
    void caiParaBrasilApiQuandoCnpjaVemVazio() {
        Contatos doBrasil = new Contatos(List.of("1140028922"), List.of());
        when(cnpja.consultar("1")).thenReturn(Contatos.vazio());
        when(brasilApi.consultar("1")).thenReturn(doBrasil);

        Contatos r = new CadeiaContatoFonte(cnpja, brasilApi).consultar("1");

        assertThat(r).isEqualTo(doBrasil);
        verify(brasilApi).consultar("1");
    }
}
