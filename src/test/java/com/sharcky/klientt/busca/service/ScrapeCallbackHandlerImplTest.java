package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.scraper.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeCallbackHandlerImplTest {

    @Mock EmpresaCacheService cacheService;
    @Mock ScrapeMapper scrapeMapper;
    @Mock AvaliadorLead avaliador;
    @Mock JobService jobService;
    @InjectMocks ScrapeCallbackHandlerImpl handler;

    @Test
    void concluidoArmazenaPontuaLigaEConclui() {
        Empresa persistida = new Empresa();
        persistida.setId(100L);
        when(scrapeMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(persistida);
        when(avaliador.avaliar(persistida)).thenReturn(new AvaliacaoLead(3.0, false, false, 0, false, 60));

        handler.tratar(callback("5", EstadoScrape.CONCLUIDO, umaEmpresa()));

        verify(cacheService).upsert(any());
        verify(jobService).registarResultado(5L, 100L, 60);
        verify(jobService).concluir(5L);
        verify(jobService, never()).marcarErro(any());
    }

    @Test
    void erroMarcaJobComoErro() {
        handler.tratar(callback("5", EstadoScrape.ERRO, List.of()));

        verify(jobService).marcarErro(5L);
        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).concluir(any());
    }

    @Test
    void buscaIdNaoNumericoArmazenaMasNaoLigaAoJob() {
        when(scrapeMapper.toEmpresa(any())).thenReturn(new Empresa());
        when(cacheService.upsert(any())).thenReturn(new Empresa());

        handler.tratar(callback("nao-numerico", EstadoScrape.CONCLUIDO, umaEmpresa()));

        verify(cacheService).upsert(any());
        verify(jobService, never()).registarResultado(any(), any(), anyInt());
        verify(jobService, never()).concluir(any());
    }

    private ScrapeCallback callback(String buscaId, EstadoScrape estado, List<EmpresaPayload> empresas) {
        return new ScrapeCallback(buscaId, estado, null, empresas);
    }

    private List<EmpresaPayload> umaEmpresa() {
        SinaisPayload s = new SinaisPayload(null, null, false, null, null, null, null, false);
        return List.of(new EmpresaPayload("Bar X", null, null, null, "Lisboa", null, null, null, "stub", s, List.of()));
    }
}
