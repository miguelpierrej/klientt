package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import com.sharcky.klientt.scraper.dto.EstadoScrape;
import com.sharcky.klientt.scraper.dto.ScrapeCallback;
import com.sharcky.klientt.scraper.ingest.ScrapeCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trata o callback do scraper: roteia pelo estado (ERRO/PARCIAL/CONCLUIDO), delega a ingestão das
 * empresas no {@link IngestaoService} (cache + score + ligação ao job) e atualiza o estado do job.
 */
@Service
public class ScrapeCallbackHandlerImpl implements ScrapeCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeCallbackHandlerImpl.class);

    private final IngestaoService ingestaoService;
    private final JobService jobService;

    public ScrapeCallbackHandlerImpl(IngestaoService ingestaoService, JobService jobService) {
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
    }

    @Override
    @Transactional
    public void tratar(ScrapeCallback callback) {
        Long jobId = parseJobId(callback.buscaId());

        if (callback.estado() == EstadoScrape.ERRO) {
            if (jobId != null) {
                jobService.marcarErro(jobId);
            }
            log.warn("Callback ERRO buscaId={} erro={}", callback.buscaId(), callback.erro());
            return;
        }

        List<EmpresaPayload> empresas = callback.empresas() != null ? callback.empresas() : List.of();
        ingestaoService.ingerir(empresas, jobId);

        if (jobId != null && callback.estado() == EstadoScrape.CONCLUIDO) {
            // Dual-fonte: o scraper é uma das fontes; o job só conclui quando todas reportarem.
            jobService.marcarFonteConcluida(jobId);
        }
        log.info("Callback {} buscaId={}: {} empresas", callback.estado(), callback.buscaId(), empresas.size());
    }

    /** O buscaId é o id do job. Tolera valores não-numéricos (ex.: testes diretos ao webhook). */
    private Long parseJobId(String buscaId) {
        try {
            return Long.valueOf(buscaId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
