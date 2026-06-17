package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.enriquecimento.client.EnriquecimentoClient;
import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoRequest;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Descoberta primária (Casa dos Dados) + arranque do enriquecimento Maps (PLANO-DUAL-FONTE.md, Fase 2).
 *
 * <p>Corre em background: busca na Casa dos Dados (NOME ou NICHO), ingere os leads e, para cada
 * empresa com CNPJ, dispara o enriquecimento Maps (assíncrono, por empresa). No fim regista quantos
 * enriquecimentos esperar; o job conclui quando todos chegarem (ou já, se nenhum).
 */
@Service
public class FonteCnpjExecutor {

    private static final Logger log = LoggerFactory.getLogger(FonteCnpjExecutor.class);

    private final ResolvedorCnae resolvedorCnae;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final JobService jobService;
    private final ClienteCnpjProperties properties;
    private final EnriquecimentoClient enriquecimentoClient;
    private final ScraperProperties scraperProperties;

    public FonteCnpjExecutor(ResolvedorCnae resolvedorCnae, FonteCnpj fonteCnpj,
                             IngestaoService ingestaoService, JobService jobService,
                             ClienteCnpjProperties properties, EnriquecimentoClient enriquecimentoClient,
                             ScraperProperties scraperProperties) {
        this.resolvedorCnae = resolvedorCnae;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
        this.properties = properties;
        this.enriquecimentoClient = enriquecimentoClient;
        this.scraperProperties = scraperProperties;
    }

    @Async
    public void executar(Long jobId, TipoBusca tipo, String termo, String regiao) {
        int enriquecimentos = 0;
        try {
            List<EmpresaPayload> empresas = descobrir(tipo, termo, regiao, properties.getLimiteDefault());
            ingestaoService.ingerir(empresas, jobId);
            enriquecimentos = dispararEnriquecimentos(jobId, empresas, regiao);
        } catch (Exception ex) {
            log.warn("Falha na descoberta CNPJ jobId={} (tipo={}, termo='{}'): {}", jobId, tipo, termo, ex.getMessage());
        } finally {
            // Conclui já se não houver enriquecimentos; senão espera os callbacks por empresa.
            jobService.marcarDescobertaConcluida(jobId, enriquecimentos);
        }
    }

    private List<EmpresaPayload> descobrir(TipoBusca tipo, String termo, String regiao, int limite) {
        if (tipo == TipoBusca.NOME) {
            return fonteCnpj.buscarPorNome(termo, regiao, limite);
        }
        List<EmpresaPayload> todas = new ArrayList<>();
        for (Cnae cnae : resolvedorCnae.resolver(termo)) {
            todas.addAll(fonteCnpj.buscarPorCnae(cnae.codigo(), regiao, limite));
        }
        return todas;
    }

    /** Dispara o enriquecimento Maps por empresa (com CNPJ). Devolve quantos foram disparados. */
    private int dispararEnriquecimentos(Long jobId, List<EmpresaPayload> empresas, String regiao) {
        int n = 0;
        String callbackUrl = scraperProperties.enriquecimentoCallbackUrl();
        for (EmpresaPayload e : empresas) {
            if (e.cnpj() == null || e.cnpj().isBlank()) {
                continue;
            }
            String municipio = e.cidade() != null ? e.cidade() : regiao;
            try {
                enriquecimentoClient.enriquecer(new EnriquecimentoRequest(
                        String.valueOf(jobId), e.cnpj(), e.nome(), municipio, e.endereco(), callbackUrl));
                n++;
            } catch (Exception ex) {
                // Dispatch falhou (scraper em baixo) — não conta como esperado, para o job não ficar preso.
                log.warn("Falha ao disparar enriquecimento cnpj={}: {}", e.cnpj(), ex.getMessage());
            }
        }
        return n;
    }
}
