package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
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
 * Trata o callback do scraper: armazena as empresas em cache, calcula o score,
 * liga-as ao job e atualiza o estado do job.
 */
@Service
public class ScrapeCallbackHandlerImpl implements ScrapeCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeCallbackHandlerImpl.class);

    private final EmpresaCacheService cacheService;
    private final ScrapeMapper scrapeMapper;
    private final AvaliadorLead avaliador;
    private final JobService jobService;

    public ScrapeCallbackHandlerImpl(EmpresaCacheService cacheService, ScrapeMapper scrapeMapper,
                                     AvaliadorLead avaliador, JobService jobService) {
        this.cacheService = cacheService;
        this.scrapeMapper = scrapeMapper;
        this.avaliador = avaliador;
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
        for (EmpresaPayload payload : empresas) {
            Empresa persistida = cacheService.upsert(scrapeMapper.toEmpresa(payload));
            if (jobId != null) {
                int score = avaliador.avaliar(persistida).score();
                jobService.registarResultado(jobId, persistida.getId(), score);
            }
        }

        if (jobId != null && callback.estado() == EstadoScrape.CONCLUIDO) {
            jobService.concluir(jobId);
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
