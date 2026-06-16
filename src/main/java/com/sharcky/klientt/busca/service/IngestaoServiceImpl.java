package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementação da ingestão partilhada (ver {@link IngestaoService}).
 * Extraída do tratamento do callback do scraper para ser reutilizada por qualquer fonte.
 */
@Service
public class IngestaoServiceImpl implements IngestaoService {

    private final EmpresaCacheService cacheService;
    private final ScrapeMapper scrapeMapper;
    private final AvaliadorLead avaliador;
    private final JobService jobService;

    public IngestaoServiceImpl(EmpresaCacheService cacheService, ScrapeMapper scrapeMapper,
                               AvaliadorLead avaliador, JobService jobService) {
        this.cacheService = cacheService;
        this.scrapeMapper = scrapeMapper;
        this.avaliador = avaliador;
        this.jobService = jobService;
    }

    @Override
    @Transactional
    public void ingerir(List<EmpresaPayload> empresas, Long jobId) {
        if (empresas == null) {
            return;
        }
        for (EmpresaPayload payload : empresas) {
            Empresa persistida = cacheService.upsert(scrapeMapper.toEmpresa(payload));
            if (jobId != null) {
                int score = avaliador.avaliar(persistida).score();
                jobService.registarResultado(jobId, persistida.getId(), score);
            }
        }
    }
}
