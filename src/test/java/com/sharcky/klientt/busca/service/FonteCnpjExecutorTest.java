package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.enriquecimento.ScraperClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FonteCnpjExecutorTest {

    /** 1ª página = franquia grátis; a busca inicial traz exatamente uma página deste tamanho. */
    private static final int TAMANHO_PAGINA = 20;

    @Mock ResolvedorCnae resolvedorCnae;
    @Mock FonteCnpj fonteCnpj;
    @Mock IngestaoService ingestaoService;
    @Mock JobService jobService;
    @Mock ScraperClient scraperClient;   // false por default → executor conclui o job na descoberta
    @Mock EnriquecimentoContatoService enriquecimentoContato;
    FonteCnpjExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new FonteCnpjExecutor(resolvedorCnae, fonteCnpj, ingestaoService, jobService,
                scraperClient, enriquecimentoContato, TAMANHO_PAGINA);
    }

    private final List<EmpresaPayload> semContato = List.of(new EmpresaPayload(
            "Barbearia X", "12345678000199", null, null, null, "São Paulo", null, null, null,
            null, List.of(), List.of(), List.of()));

    @Test
    void nichoComCnaeConfirmadoBuscaDiretoSemResolver() {
        when(fonteCnpj.buscarPaginaPorCnae("9602501", "São Paulo", null, TAMANHO_PAGINA)).thenReturn(new FonteCnpj.Pagina(semContato, "cur1"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(ingestaoService).ingerir(semContato, 7L);
        verify(resolvedorCnae, never()).resolver(any());   // CNAE já confirmado
        verify(jobService).concluir(7L);
    }

    @Test
    void nichoSemCnaeUsaResolverComoFallback() {
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae("9602-5/01", "São Paulo", TAMANHO_PAGINA)).thenReturn(semContato);

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", null);

        verify(ingestaoService).ingerir(semContato, 7L);
        verify(jobService).concluir(7L);
    }

    @Test
    void nomeBuscaTextualSemResolverCnae() {
        when(fonteCnpj.buscarPorNome("Barbearia do Zé", "São Paulo", TAMANHO_PAGINA)).thenReturn(semContato);

        executor.executar(7L, TipoBusca.NOME, "Barbearia do Zé", "São Paulo", null);

        verify(ingestaoService).ingerir(semContato, 7L);
        verify(resolvedorCnae, never()).resolver(any());
        verify(jobService).concluir(7L);
    }

    @Test
    void nichoSemCnaeResolvidoNaoBuscaMasConclui() {
        when(resolvedorCnae.resolver("nicho desconhecido")).thenReturn(List.of());

        executor.executar(7L, TipoBusca.NICHO, "nicho desconhecido", "São Paulo", null);

        verify(fonteCnpj, never()).buscarPorCnae(any(), any(), anyInt());
        verify(jobService).concluir(7L);
    }

    @Test
    void concluiMesmoComErro() {
        when(fonteCnpj.buscarPaginaPorCnae(any(), any(), any(), anyInt())).thenThrow(new RuntimeException("API caiu"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(jobService).concluir(7L);   // falha graciosa
    }

    @Test
    void quandoScraperDespachadoNaoConcluiJob() {
        when(fonteCnpj.buscarPaginaPorCnae("9602501", "São Paulo", null, TAMANHO_PAGINA)).thenReturn(new FonteCnpj.Pagina(semContato, "cur1"));
        when(scraperClient.enriquecer(7L, semContato)).thenReturn(true);   // scraper aceitou

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(scraperClient).enriquecer(7L, semContato);
        verify(jobService, never()).concluir(7L);   // o callback do scraper concluirá o job
    }

    @Test
    void despachaEnriquecimentoDeContatoEmBackground() {
        when(fonteCnpj.buscarPaginaPorCnae("9602501", "São Paulo", null, TAMANHO_PAGINA)).thenReturn(new FonteCnpj.Pagina(semContato, "cur1"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        // Fora do caminho crítico: delega, não bloqueia (o serviço é @Async).
        verify(enriquecimentoContato).enriquecer(semContato);
    }

    @Test
    void naoEnriqueceQuandoADescobertaFalha() {
        when(fonteCnpj.buscarPaginaPorCnae(any(), any(), any(), anyInt())).thenThrow(new RuntimeException("API caiu"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(enriquecimentoContato, never()).enriquecer(any());
    }
}
