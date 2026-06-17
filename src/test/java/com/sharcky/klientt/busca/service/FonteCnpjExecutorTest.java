package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
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
    @InjectMocks FonteCnpjExecutor executor;

    @Test
    void resolveBuscaIngereEMarcaFonteConcluida() {
        List<EmpresaPayload> empresas = List.of(new EmpresaPayload(
                "Barbearia X", null, null, null, null, "São Paulo", null, null, null, "casadosdados", null, List.of(), null));
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae("9602-5/01", "São Paulo", 25)).thenReturn(empresas);   // usa o limite da fonte CNPJ

        executor.executar(7L, "barbearias", "São Paulo");

        verify(ingestaoService).ingerir(empresas, 7L);
        verify(jobService).marcarFonteConcluida(7L);
    }

    @Test
    void semCnaeResolvidoNaoBuscaMasMarcaFonteConcluida() {
        when(resolvedorCnae.resolver("nicho desconhecido")).thenReturn(List.of());

        executor.executar(7L, "nicho desconhecido", "São Paulo");

        verify(fonteCnpj, never()).buscarPorCnae(any(), any(), anyInt());
        verify(ingestaoService, never()).ingerir(any(), any());
        verify(jobService).marcarFonteConcluida(7L);   // job não fica preso
    }

    @Test
    void marcaFonteConcluidaMesmoComErro() {
        when(properties.getLimiteDefault()).thenReturn(25);
        when(resolvedorCnae.resolver("barbearias")).thenReturn(List.of(new Cnae("9602-5/01", "Cabeleireiros")));
        when(fonteCnpj.buscarPorCnae(any(), any(), anyInt())).thenThrow(new RuntimeException("API caiu"));

        executor.executar(7L, "barbearias", "São Paulo");

        verify(jobService).marcarFonteConcluida(7L);   // falha graciosa
    }
}
