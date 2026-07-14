package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.mapper.EmpresaPayloadMapper;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestaoServiceImplTest {

    @Mock EmpresaCacheService cacheService;
    @Mock EmpresaPayloadMapper payloadMapper;
    @Mock JobService jobService;
    @InjectMocks IngestaoServiceImpl ingestao;

    @Test
    void comJobIdFazUpsertELiga() {
        Empresa persistida = new Empresa();
        persistida.setId(100L);
        when(payloadMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(persistida);

        ingestao.ingerir(umaEmpresa(), 5L);

        verify(cacheService).upsert(any());
        verify(jobService).registarResultado(5L, 100L);
    }

    @Test
    void semJobIdApenasFazCache() {
        when(payloadMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(new Empresa());

        ingestao.ingerir(umaEmpresa(), null);

        verify(cacheService).upsert(any());
        verify(jobService, never()).registarResultado(any(), any());
    }

    @Test
    void listaVaziaNaoFazNada() {
        ingestao.ingerir(List.of(), 5L);

        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).registarResultado(any(), any());
    }

    @Test
    void listaNulaNaoFazNada() {
        ingestao.ingerir(null, 5L);

        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).registarResultado(any(), any());
    }

    private List<EmpresaPayload> umaEmpresa() {
        return List.of(new EmpresaPayload("Bar X", null, null, "contato@barx.test", null,
                "Lisboa", null, null, null, null, List.of(), List.of("contato@barx.test"), List.of()));
    }
}
