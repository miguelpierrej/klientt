package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import com.sharcky.klientt.scraper.dto.SinaisPayload;
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
class IngestaoServiceImplTest {

    @Mock EmpresaCacheService cacheService;
    @Mock ScrapeMapper scrapeMapper;
    @Mock AvaliadorLead avaliador;
    @Mock JobService jobService;
    @InjectMocks IngestaoServiceImpl ingestao;

    @Test
    void comJobIdFazUpsertPontuaELiga() {
        Empresa persistida = new Empresa();
        persistida.setId(100L);
        when(scrapeMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(persistida);
        when(avaliador.avaliar(persistida)).thenReturn(new AvaliacaoLead(3.0, false, false, 0, true, false, 60));

        ingestao.ingerir(umaEmpresa(), 5L);

        verify(cacheService).upsert(any());
        verify(jobService).registarResultado(5L, 100L, 60);
    }

    @Test
    void semJobIdApenasFazCache() {
        when(scrapeMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(new Empresa());

        ingestao.ingerir(umaEmpresa(), null);

        verify(cacheService).upsert(any());
        verify(jobService, never()).registarResultado(any(), any(), anyInt());
    }

    @Test
    void listaVaziaNaoFazNada() {
        ingestao.ingerir(List.of(), 5L);

        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).registarResultado(any(), any(), anyInt());
    }

    @Test
    void listaNulaNaoFazNada() {
        ingestao.ingerir(null, 5L);

        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).registarResultado(any(), any(), anyInt());
    }

    private List<EmpresaPayload> umaEmpresa() {
        SinaisPayload s = new SinaisPayload(null, null, false, null, null, null, null, false);
        return List.of(new EmpresaPayload("Bar X", null, null, "contato@barx.test", null,
                "Lisboa", null, null, null, "stub", s, List.of(), null));
    }
}
