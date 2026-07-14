package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.DescobertaProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FonteDescobertaRouterTest {

    @Mock MinhaReceitaFonte minhaReceita;
    @Mock ApiGeridaCnpjFonte casaDosDados;

    private final EmpresaPayload umLead = new EmpresaPayload(
            "Acme", "12345678000199", null, null, null, "SP", null, null, null,
            null, List.of(), List.of(), List.of());

    private FonteDescobertaRouter router(String fonte, boolean fallback) {
        DescobertaProperties props = new DescobertaProperties();
        props.setFonte(fonte);
        props.setFallbackCasadosdados(fallback);
        return new FonteDescobertaRouter(minhaReceita, casaDosDados, props);
    }

    @Test
    void cnaeVaiParaMinhaReceitaPorDefault() {
        when(minhaReceita.buscarPorCnae("4712100", "SP", 25)).thenReturn(List.of(umLead));

        router("minhareceita", true).buscarPorCnae("4712100", "SP", 25);

        verify(minhaReceita).buscarPorCnae("4712100", "SP", 25);
        verify(casaDosDados, never()).buscarPorCnae(any(), any(), anyInt());   // teve resultado → sem fallback
    }

    @Test
    void cnaeVaiParaCasaDosDadosQuandoConfigurado() {
        router("casadosdados", true).buscarPorCnae("4712100", "SP", 25);

        verify(casaDosDados).buscarPorCnae("4712100", "SP", 25);
        verify(minhaReceita, never()).buscarPorCnae(any(), any(), anyInt());
    }

    @Test
    void nomeVaiSempreParaCasaDosDados() {
        when(casaDosDados.buscarPorNome("Acme", "SP", 10)).thenReturn(List.<EmpresaPayload>of());

        router("minhareceita", true).buscarPorNome("Acme", "SP", 10);

        verify(casaDosDados).buscarPorNome("Acme", "SP", 10);
        verify(minhaReceita, never()).buscarPorNome(any(), any(), anyInt());
    }

    @Test
    void cnaeVazioSemErroNaoCaiParaCasaDosDados() {
        // "Sem resultados" (200 vazio) NÃO é falha → não gasta a Casa dos Dados.
        when(minhaReceita.buscarPorCnae("4712100", "SP", 25)).thenReturn(List.of());

        List<EmpresaPayload> res = router("minhareceita", true).buscarPorCnae("4712100", "SP", 25);

        assertThat(res).isEmpty();
        verify(casaDosDados, never()).buscarPorCnae(any(), any(), anyInt());
    }

    @Test
    void cnaeComFalhaRealNaMinhaReceitaCaiParaCasaDosDados() {
        when(minhaReceita.buscarPorCnae("4712100", "SP", 25))
                .thenThrow(new FonteDescobertaException("fora do ar", null));
        when(casaDosDados.buscarPorCnae("4712100", "SP", 25)).thenReturn(List.of(umLead));

        List<EmpresaPayload> res = router("minhareceita", true).buscarPorCnae("4712100", "SP", 25);

        assertThat(res).containsExactly(umLead);
        verify(casaDosDados).buscarPorCnae("4712100", "SP", 25);
    }

    @Test
    void falhaSemFallbackDevolveVazioSemCasaDosDados() {
        when(minhaReceita.buscarPorCnae("4712100", "SP", 25))
                .thenThrow(new FonteDescobertaException("fora do ar", null));

        List<EmpresaPayload> res = router("minhareceita", false).buscarPorCnae("4712100", "SP", 25);

        assertThat(res).isEmpty();
        verify(casaDosDados, never()).buscarPorCnae(any(), any(), anyInt());
    }

    @Test
    void paginaInicialComFalhaCaiParaCasaDosDados() {
        when(minhaReceita.buscarPaginaPorCnae("4712100", "SP", null, 20))
                .thenThrow(new FonteDescobertaException("fora do ar", null));
        when(casaDosDados.buscarPaginaPorCnae("4712100", "SP", null, 20))
                .thenReturn(new FonteCnpj.Pagina(List.of(umLead), null));

        FonteCnpj.Pagina p = router("minhareceita", true).buscarPaginaPorCnae("4712100", "SP", null, 20);

        assertThat(p.empresas()).containsExactly(umLead);
        verify(casaDosDados).buscarPaginaPorCnae("4712100", "SP", null, 20);
    }

    @Test
    void paginaInicialVaziaSemErroNaoCaiParaCasaDosDados() {
        when(minhaReceita.buscarPaginaPorCnae("4712100", "SP", null, 20))
                .thenReturn(new FonteCnpj.Pagina(List.of(), null));

        FonteCnpj.Pagina p = router("minhareceita", true).buscarPaginaPorCnae("4712100", "SP", null, 20);

        assertThat(p.empresas()).isEmpty();
        verify(casaDosDados, never()).buscarPaginaPorCnae(any(), any(), any(), anyInt());
    }

    @Test
    void carregarMaisComFalhaNaoAcionaFallback() {
        // Com cursor != null (continuação), NÃO cai para a Casa dos Dados mesmo em falha.
        when(minhaReceita.buscarPaginaPorCnae("4712100", "SP", "cur1", 20))
                .thenThrow(new FonteDescobertaException("fora do ar", null));

        FonteCnpj.Pagina p = router("minhareceita", true).buscarPaginaPorCnae("4712100", "SP", "cur1", 20);

        assertThat(p.empresas()).isEmpty();
        verify(casaDosDados, never()).buscarPaginaPorCnae(any(), any(), any(), anyInt());
    }
}
