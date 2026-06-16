package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.scraper.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeCallbackHandlerImplTest {

    @Mock IngestaoService ingestaoService;
    @Mock JobService jobService;
    @InjectMocks ScrapeCallbackHandlerImpl handler;

    @Test
    void concluidoIngereEConclui() {
        List<EmpresaPayload> empresas = umaEmpresa();

        handler.tratar(callback("5", EstadoScrape.CONCLUIDO, empresas));

        verify(ingestaoService).ingerir(empresas, 5L);
        verify(jobService).marcarFonteConcluida(5L);
        verify(jobService, never()).marcarErro(any());
    }

    @Test
    void erroMarcaJobComoErroSemIngerir() {
        handler.tratar(callback("5", EstadoScrape.ERRO, List.of()));

        verify(jobService).marcarErro(5L);
        verify(ingestaoService, never()).ingerir(any(), any());
        verify(jobService, never()).concluir(any());
    }

    @Test
    void parcialIngereSemConcluir() {
        List<EmpresaPayload> empresas = umaEmpresa();

        handler.tratar(callback("5", EstadoScrape.PARCIAL, empresas));

        verify(ingestaoService).ingerir(empresas, 5L);
        verify(jobService, never()).concluir(any());
        verify(jobService, never()).marcarErro(any());
    }

    @Test
    void buscaIdNaoNumericoIngereSemLigarAoJob() {
        List<EmpresaPayload> empresas = umaEmpresa();

        handler.tratar(callback("nao-numerico", EstadoScrape.CONCLUIDO, empresas));

        verify(ingestaoService).ingerir(eq(empresas), eq(null));
        verify(jobService, never()).concluir(any());
    }

    private ScrapeCallback callback(String buscaId, EstadoScrape estado, List<EmpresaPayload> empresas) {
        return new ScrapeCallback(buscaId, estado, null, empresas);
    }

    private List<EmpresaPayload> umaEmpresa() {
        SinaisPayload s = new SinaisPayload(null, null, false, null, null, null, null, false);
        return List.of(new EmpresaPayload("Bar X", null, null, "contato@barx.test", null,
                "Lisboa", null, null, null, "stub", s, List.of(), null));
    }
}
