package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.enriquecimento.client.EnriquecimentoClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FonteCnpjExecutorTest {

    @Mock ResolvedorCnae resolvedorCnae;
    @Mock FonteCnpj fonteCnpj;
    @Mock IngestaoService ingestaoService;
    @Mock JobService jobService;
    @Mock ClienteCnpjProperties properties;
    @Mock EnriquecimentoClient enriquecimentoClient;
    @Mock ScraperProperties scraperProperties;
    @InjectMocks FonteCnpjExecutor executor;

    private final List<EmpresaPayload> empresas = List.of(new EmpresaPayload(
            "Barbearia X", "12345678000199", null, null, null, "São Paulo", null, null, null,
            "casadosdados", null, List.of(), null));

    @Test
    void nichoIngereDisparaEnriquecimentoEMarcaDescoberta() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae("9602-5/01", "São Paulo", 25)).thenReturn(empresas);

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo");

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(enriquecimentoClient).enriquecer(any());                  // 1 empresa com CNPJ
        verify(jobService).marcarDescobertaConcluida(7L, 1);             // 1 enriquecimento esperado
    }

    @Test
    void nomeBuscaTextualSemResolverCnae() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(fonteCnpj.buscarPorNome("Barbearia do Zé", "São Paulo", 25)).thenReturn(empresas);

        executor.executar(7L, TipoBusca.NOME, "Barbearia do Zé", "São Paulo");

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(resolvedorCnae, never()).resolver(any());
        verify(jobService).marcarDescobertaConcluida(7L, 1);
    }

    @Test
    void nichoSemCnaeResolvidoMarcaDescobertaComZero() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("nicho desconhecido")).thenReturn(List.of());

        executor.executar(7L, TipoBusca.NICHO, "nicho desconhecido", "São Paulo");

        verify(fonteCnpj, never()).buscarPorCnae(any(), any(), anyInt());
        verify(enriquecimentoClient, never()).enriquecer(any());
        verify(jobService).marcarDescobertaConcluida(7L, 0);   // sem empresas → conclui já
    }

    @Test
    void marcaDescobertaMesmoComErro() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae(any(), any(), anyInt())).thenThrow(new RuntimeException("API caiu"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo");

        verify(jobService).marcarDescobertaConcluida(7L, 0);   // falha graciosa
    }
}
