package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.DescobertaProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FonteDescobertaRouterTest {

    @Mock MinhaReceitaFonte minhaReceita;
    @Mock ApiGeridaCnpjFonte casaDosDados;

    private FonteDescobertaRouter router(String fonte) {
        DescobertaProperties props = new DescobertaProperties();
        props.setFonte(fonte);
        return new FonteDescobertaRouter(minhaReceita, casaDosDados, props);
    }

    @Test
    void cnaeVaiParaMinhaReceitaPorDefault() {
        router("minhareceita").buscarPorCnae("4712100", "SP", 25);

        verify(minhaReceita).buscarPorCnae("4712100", "SP", 25);
        verify(casaDosDados, never()).buscarPorCnae(any(), any(), anyInt());
    }

    @Test
    void cnaeVaiParaCasaDosDadosQuandoConfigurado() {
        router("casadosdados").buscarPorCnae("4712100", "SP", 25);

        verify(casaDosDados).buscarPorCnae("4712100", "SP", 25);
        verify(minhaReceita, never()).buscarPorCnae(any(), any(), anyInt());
    }

    @Test
    void nomeVaiSempreParaCasaDosDados() {
        when(casaDosDados.buscarPorNome("Acme", "SP", 10)).thenReturn(List.<EmpresaPayload>of());

        router("minhareceita").buscarPorNome("Acme", "SP", 10);

        verify(casaDosDados).buscarPorNome("Acme", "SP", 10);
        verify(minhaReceita, never()).buscarPorNome(any(), any(), anyInt());
    }
}
