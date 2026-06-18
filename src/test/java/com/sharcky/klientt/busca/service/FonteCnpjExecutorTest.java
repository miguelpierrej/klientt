package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
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
    @InjectMocks FonteCnpjExecutor executor;

    private final List<EmpresaPayload> empresas = List.of(new EmpresaPayload(
            "Barbearia X", "12345678000199", null, null, null, "São Paulo", null, null, null,
            null, List.of(), List.of()));

    @Test
    void nichoComCnaeConfirmadoBuscaDiretoSemResolver() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(fonteCnpj.buscarPorCnae("9602501", "São Paulo", 25)).thenReturn(empresas);

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(resolvedorCnae, never()).resolver(any());   // CNAE já confirmado
        verify(jobService).concluir(7L);
    }

    @Test
    void nichoSemCnaeUsaResolverComoFallback() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae("9602-5/01", "São Paulo", 25)).thenReturn(empresas);

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", null);

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(jobService).concluir(7L);
    }

    @Test
    void nomeBuscaTextualSemResolverCnae() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(fonteCnpj.buscarPorNome("Barbearia do Zé", "São Paulo", 25)).thenReturn(empresas);

        executor.executar(7L, TipoBusca.NOME, "Barbearia do Zé", "São Paulo", null);

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(resolvedorCnae, never()).resolver(any());
        verify(jobService).concluir(7L);
    }

    @Test
    void nichoSemCnaeResolvidoNaoBuscaMasConclui() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("nicho desconhecido")).thenReturn(List.of());

        executor.executar(7L, TipoBusca.NICHO, "nicho desconhecido", "São Paulo", null);

        verify(fonteCnpj, never()).buscarPorCnae(any(), any(), anyInt());
        verify(jobService).concluir(7L);
    }

    @Test
    void concluiMesmoComErro() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(fonteCnpj.buscarPorCnae(any(), any(), anyInt())).thenThrow(new RuntimeException("API caiu"));

        executor.executar(7L, TipoBusca.NICHO, "barbearias", "São Paulo", "9602501");

        verify(jobService).concluir(7L);   // falha graciosa
    }
}
