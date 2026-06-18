package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Descoberta (Casa dos Dados) em background (PLANO-SO-API.md, Fase A).
 *
 * <p>Corre em {@code @Async}: busca na Casa dos Dados (NOME ou NICHO), ingere os leads e conclui o
 * job. Falha graciosa: qualquer erro é registado e o job conclui na mesma (lista vazia).
 */
@Service
public class FonteCnpjExecutor {

    private static final Logger log = LoggerFactory.getLogger(FonteCnpjExecutor.class);

    private final ResolvedorCnae resolvedorCnae;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final JobService jobService;
    private final ClienteCnpjProperties properties;

    public FonteCnpjExecutor(ResolvedorCnae resolvedorCnae, FonteCnpj fonteCnpj,
                             IngestaoService ingestaoService, JobService jobService,
                             ClienteCnpjProperties properties) {
        this.resolvedorCnae = resolvedorCnae;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
        this.properties = properties;
    }

    @Async
    public void executar(Long jobId, TipoBusca tipo, String termo, String regiao, String cnae) {
        try {
            List<EmpresaPayload> empresas = descobrir(tipo, termo, regiao, cnae, properties.getLimiteDefault());
            ingestaoService.ingerir(empresas, jobId);
        } catch (Exception ex) {
            log.warn("Falha na descoberta CNPJ jobId={} (tipo={}, termo='{}'): {}", jobId, tipo, termo, ex.getMessage());
        } finally {
            jobService.concluir(jobId);
        }
    }

    private List<EmpresaPayload> descobrir(TipoBusca tipo, String termo, String regiao, String cnae, int limite) {
        if (tipo == TipoBusca.NOME) {
            return fonteCnpj.buscarPorNome(termo, regiao, limite);
        }
        // NICHO: usa o CNAE confirmado pelo utilizador; sem ele, resolve o termo (fallback).
        if (cnae != null && !cnae.isBlank()) {
            return fonteCnpj.buscarPorCnae(cnae, regiao, limite);
        }
        List<EmpresaPayload> todas = new ArrayList<>();
        for (Cnae c : resolvedorCnae.resolver(termo)) {
            todas.addAll(fonteCnpj.buscarPorCnae(c.codigo(), regiao, limite));
        }
        return todas;
    }
}
