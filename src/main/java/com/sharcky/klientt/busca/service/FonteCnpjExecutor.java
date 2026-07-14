package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.FonteContatoCnpj;
import com.sharcky.klientt.cnpj.config.ContatoFallbackProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.enriquecimento.ScraperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Descoberta (Casa dos Dados) em background (PLANO-SO-API.md, Fases A/D).
 *
 * <p>Corre em {@code @Async}: busca na Casa dos Dados (NOME ou NICHO), ingere os leads e, para os que
 * vieram sem contacto, tenta o fallback por CNPJ (BrasilAPI). No fim conclui o job. Falha graciosa:
 * qualquer erro é registado e o job conclui na mesma.
 */
@Service
public class FonteCnpjExecutor {

    private static final Logger log = LoggerFactory.getLogger(FonteCnpjExecutor.class);

    private final ResolvedorCnae resolvedorCnae;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final JobService jobService;
    private final FonteContatoCnpj fonteContato;
    private final ContatoFallbackProperties contatoFallback;
    private final EmpresaCacheService cacheService;
    private final ScraperClient scraperClient;
    /** Tamanho da 1ª página (= franquia grátis). A busca inicial traz exatamente uma página. */
    private final int tamanhoPagina;

    public FonteCnpjExecutor(ResolvedorCnae resolvedorCnae, FonteCnpj fonteCnpj,
                             IngestaoService ingestaoService, JobService jobService,
                             FonteContatoCnpj fonteContato,
                             ContatoFallbackProperties contatoFallback, EmpresaCacheService cacheService,
                             ScraperClient scraperClient,
                             @Value("${klientt.busca.tamanho-pagina:20}") int tamanhoPagina) {
        this.resolvedorCnae = resolvedorCnae;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
        this.fonteContato = fonteContato;
        this.contatoFallback = contatoFallback;
        this.cacheService = cacheService;
        this.scraperClient = scraperClient;
        this.tamanhoPagina = tamanhoPagina;
    }

    @Async
    public void executar(Long jobId, TipoBusca tipo, String termo, String regiao, String cnae) {
        List<EmpresaPayload> empresas;
        try {
            // 1ª página = uma página (tamanho-pagina), que é a franquia grátis. "Carregar mais"
            // (BuscaServiceImpl) é que traz lotes de limite-default e consome créditos.
            FonteCnpj.Pagina pagina = descobrir(tipo, termo, regiao, cnae, tamanhoPagina);
            empresas = pagina.empresas();
            ingestaoService.ingerir(empresas, jobId);
            complementarContatos(empresas);
            jobService.registarCursor(jobId, pagina.cursor());   // cursor p/ "carregar mais"
        } catch (Exception ex) {
            log.warn("Falha na descoberta CNPJ jobId={} (tipo={}, termo='{}'): {}", jobId, tipo, termo, ex.getMessage());
            jobService.concluir(jobId);
            return;
        }
        // Enriquecimento por scraper (Novo Fluxo): se despachado, o callback conclui o job;
        // se está desligado/indisponível, conclui-se já na descoberta (comportamento só-API).
        if (!scraperClient.enriquecer(jobId, empresas)) {
            jobService.concluir(jobId);
        }
    }

    private FonteCnpj.Pagina descobrir(TipoBusca tipo, String termo, String regiao, String cnae, int limite) {
        if (tipo == TipoBusca.NOME) {
            return new FonteCnpj.Pagina(fonteCnpj.buscarPorNome(termo, regiao, limite), null);
        }
        // NICHO: usa o CNAE confirmado pelo utilizador (com cursor p/ carregar mais); sem ele, resolve.
        if (cnae != null && !cnae.isBlank()) {
            return fonteCnpj.buscarPaginaPorCnae(cnae, regiao, null, limite);
        }
        List<EmpresaPayload> todas = new ArrayList<>();
        for (Cnae c : resolvedorCnae.resolver(termo)) {
            todas.addAll(fonteCnpj.buscarPorCnae(c.codigo(), regiao, limite));
        }
        return new FonteCnpj.Pagina(todas, null);
    }

    /**
     * Para cada empresa com CNPJ mas sem contacto, consulta a API pública por CNPJ e funde os
     * contactos encontrados (cada upsert na sua transação). Desligado por default.
     */
    private void complementarContatos(List<EmpresaPayload> empresas) {
        if (!contatoFallback.isEnabled()) {
            return;
        }
        int preenchidos = 0;
        for (EmpresaPayload e : empresas) {
            if (temContato(e) || e.cnpj() == null || e.cnpj().isBlank()) {
                continue;
            }
            FonteContatoCnpj.Contatos extra = fonteContato.consultar(e.cnpj());
            if (extra.isVazio()) {
                continue;
            }
            cacheService.upsert(patchDeContatos(e, extra));
            preenchidos++;
        }
        if (preenchidos > 0) {
            log.info("Fallback de contacto preencheu {} de {} empresas sem contacto", preenchidos, empresas.size());
        }
    }

    private static boolean temContato(EmpresaPayload e) {
        return (e.telefones() != null && !e.telefones().isEmpty())
                || (e.emails() != null && !e.emails().isEmpty());
    }

    /** Empresa mínima (identificada por CNPJ) só com os contactos do fallback, para o merge da cache. */
    private static Empresa patchDeContatos(EmpresaPayload e, FonteContatoCnpj.Contatos extra) {
        Empresa patch = new Empresa();
        patch.setNome(e.nome());
        patch.setCidade(e.cidade());
        patch.setCnpj(e.cnpj());
        extra.telefones().forEach(t -> patch.adicionarContato(contato("telefone", t)));
        extra.emails().forEach(m -> patch.adicionarContato(contato("email", m)));
        if (!extra.telefones().isEmpty()) {
            patch.setTelefone(extra.telefones().get(0));
        }
        if (!extra.emails().isEmpty()) {
            patch.setEmail(extra.emails().get(0));
        }
        return patch;
    }

    private static Contato contato(String tipo, String valor) {
        Contato c = new Contato();
        c.setTipo(tipo);
        c.setValor(valor);
        return c;
    }
}
