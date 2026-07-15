package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
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
 * <p>Corre em {@code @Async}: busca (NOME ou NICHO), ingere os leads e conclui o job logo na
 * descoberta (resultados rápidos). O enriquecimento de contato corre depois, em background
 * ({@link EnriquecimentoContatoService}), fora do caminho crítico. Falha graciosa: qualquer erro é
 * registado e o job conclui na mesma.
 */
@Service
public class FonteCnpjExecutor {

    private static final Logger log = LoggerFactory.getLogger(FonteCnpjExecutor.class);

    private final ResolvedorCnae resolvedorCnae;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final JobService jobService;
    private final ScraperClient scraperClient;
    private final EnriquecimentoContatoService enriquecimentoContato;
    /** Tamanho da 1ª página (= franquia grátis). A busca inicial traz exatamente uma página. */
    private final int tamanhoPagina;

    public FonteCnpjExecutor(ResolvedorCnae resolvedorCnae, FonteCnpj fonteCnpj,
                             IngestaoService ingestaoService, JobService jobService,
                             ScraperClient scraperClient, EnriquecimentoContatoService enriquecimentoContato,
                             @Value("${klientt.busca.tamanho-pagina:20}") int tamanhoPagina) {
        this.resolvedorCnae = resolvedorCnae;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.jobService = jobService;
        this.scraperClient = scraperClient;
        this.enriquecimentoContato = enriquecimentoContato;
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
        // Contatos em falta: preenchidos em BACKGROUND (throttle 5/min fora do caminho crítico) —
        // o utilizador vê a lista imediatamente e os contatos aparecem à medida que chegam.
        enriquecimentoContato.enriquecer(empresas);
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
}
