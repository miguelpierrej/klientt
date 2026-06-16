package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Executa, em paralelo com o scraper, a fonte de descoberta por CNAE (PLANO-DUAL-FONTE.md, Fase E):
 * resolve nicho→CNAE, busca empresas na {@link FonteCnpj}, ingere-as ({@link IngestaoService}) e, no
 * fim, marca a fonte como concluída (mesmo em erro/sem resultados) para o job poder concluir.
 *
 * <p>Corre num thread próprio (@Async) para não bloquear o arranque da busca — o job já foi
 * persistido (commit) antes desta chamada.
 */
@Service
public class FonteCnpjExecutor {

    private static final Logger log = LoggerFactory.getLogger(FonteCnpjExecutor.class);

    private final ResolvedorCnae resolvedorCnae;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final JobService jobService;

    public FonteCnpjExecutor(ResolvedorCnae resolvedorCnae, FonteCnpj fonteCnpj,
                             IngestaoService ingestaoService, JobService jobService) {
        this.resolvedorCnae = resolvedorCnae;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
    }

    @Async
    public void executar(Long jobId, String termo, String regiao, int limite) {
        try {
            for (Cnae cnae : resolvedorCnae.resolver(termo)) {
                ingestaoService.ingerir(fonteCnpj.buscarPorCnae(cnae.codigo(), regiao, limite), jobId);
            }
        } catch (Exception ex) {
            log.warn("Falha na fonte CNPJ para jobId={} (termo='{}'): {}", jobId, termo, ex.getMessage());
        } finally {
            jobService.marcarFonteConcluida(jobId);
        }
    }
}
