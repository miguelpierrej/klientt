package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.dto.ResultadoBusca;
import com.sharcky.klientt.busca.job.EstadoJob;
import com.sharcky.klientt.busca.job.JobBusca;
import com.sharcky.klientt.busca.job.JobResultado;
import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.LeadDetalheMapper;
import com.sharcky.klientt.busca.mapper.LeadMapper;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.conta.service.QuotaService;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.ScrapeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Orquestra o fluxo assíncrono de busca (ARQUITETURA §4): cria o job, dispara o
 * scraper e, no polling, devolve o estado e (quando concluído) os leads pontuados.
 */
@Service
public class BuscaServiceImpl implements BuscaService {

    private static final Logger log = LoggerFactory.getLogger(BuscaServiceImpl.class);
    private static final double NOTA_BAIXA = 4.0;
    private static final int POUCOS_SEGUIDORES = 500;

    private final JobService jobService;
    private final QuotaService quotaService;
    private final ScraperClient scraperClient;
    private final FonteCnpjExecutor fonteCnpjExecutor;
    private final ScraperProperties properties;
    private final JobResultadoRepository jobResultadoRepository;
    private final EmpresaRepository empresaRepository;
    private final AvaliadorLead avaliador;
    private final LeadMapper leadMapper;
    private final LeadDetalheMapper detalheMapper;

    public BuscaServiceImpl(JobService jobService, QuotaService quotaService, ScraperClient scraperClient,
                            FonteCnpjExecutor fonteCnpjExecutor, ScraperProperties properties,
                            JobResultadoRepository jobResultadoRepository,
                            EmpresaRepository empresaRepository, AvaliadorLead avaliador,
                            LeadMapper leadMapper, LeadDetalheMapper detalheMapper) {
        this.jobService = jobService;
        this.quotaService = quotaService;
        this.scraperClient = scraperClient;
        this.fonteCnpjExecutor = fonteCnpjExecutor;
        this.properties = properties;
        this.jobResultadoRepository = jobResultadoRepository;
        this.empresaRepository = empresaRepository;
        this.avaliador = avaliador;
        this.leadMapper = leadMapper;
        this.detalheMapper = detalheMapper;
    }

    @Override
    public Long iniciar(BuscaRequest request, Long utilizadorId) {
        quotaService.garantirDisponibilidade(utilizadorId);
        // criar() é transacional e faz commit antes de dispararmos as fontes — assim o callback
        // assíncrono do scraper e a fonte CNPJ encontram sempre o job já persistido.
        Long jobId = jobService.criar(request, utilizadorId);
        // Duas fontes em paralelo: scraper (Maps, assíncrono via callback) + CNPJ-por-CNAE (assíncrono).
        dispararScraper(jobId, request);
        fonteCnpjExecutor.executar(jobId, request.termo(), request.regiao());
        return jobId;
    }

    private void dispararScraper(Long jobId, BuscaRequest request) {
        // cnae=null: a resolução nicho→CNAE corre de forma assíncrona na FonteCnpj; o scraper
        // (Maps) ignora o campo. Fica plumbado no contrato para uso futuro.
        ScrapeRequest scrapeRequest = new ScrapeRequest(
                String.valueOf(jobId), request.tipo(), request.termo(), request.regiao(), null,
                properties.getLimiteDefault(), properties.getTamanhoLote(), properties.isColetarEmails(),
                properties.isVerificarSmtp(), properties.callbackUrl());
        try {
            scraperClient.iniciarBusca(scrapeRequest);
        } catch (Exception ex) {
            // Falha graciosa (dual-fonte, CONTRATO §7.1): o scraper em baixo não mata o job —
            // marca-se esta fonte como concluída e a fonte CNPJ ainda pode completar a busca.
            log.warn("Scraper indisponível para jobId={} ({}): a busca continua só com a fonte CNPJ",
                    jobId, ex.getMessage());
            jobService.marcarFonteConcluida(jobId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoBusca consultar(Long jobId, Long utilizadorId) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);

        if (job.getEstado() == EstadoJob.ERRO) {
            return new ResultadoBusca(jobId, job.getTermo(), job.getEstado(), List.of(), null);
        }

        List<LeadResponse> leads = avaliadosDoJob(jobId).stream()
                .sorted(comparador(OrdenarPor.SCORE))
                .map(la -> leadMapper.toResponse(la.empresa(), la.avaliacao()))
                .toList();

        return new ResultadoBusca(jobId, job.getTermo(), job.getEstado(), leads, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadResponse> filtrar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        return aplicar(jobId, utilizadorId, filtro).stream()
                .map(la -> leadMapper.toResponse(la.empresa(), la.avaliacao()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadDetalhe> exportar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        return aplicar(jobId, utilizadorId, filtro).stream()
                .map(la -> detalheMapper.toDetalhe(la.empresa(), la.avaliacao()))
                .toList();
    }

    /** Aplica filtros e ordenação aos leads de um job concluído (do utilizador). */
    private List<LeadAvaliado> aplicar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);
        if (job.getEstado() != EstadoJob.CONCLUIDO) {
            return List.of();
        }
        return avaliadosDoJob(jobId).stream()
                .filter(la -> !filtro.semSite() || !la.avaliacao().temSite())
                .filter(la -> !filtro.notaBaixa() || la.avaliacao().notaGoogle() < NOTA_BAIXA)
                .filter(la -> !filtro.poucosSeguidores()
                        || (la.avaliacao().seguidoresConhecidos() && la.avaliacao().seguidores() < POUCOS_SEGUIDORES))
                .filter(la -> !filtro.procon() || la.avaliacao().proconEviteSite())
                .filter(la -> !filtro.comContato() || la.empresa().isContactavel())
                .sorted(comparador(filtro.ordenarOuPadrao()))
                .toList();
    }

    private Comparator<LeadAvaliado> comparador(OrdenarPor ordenar) {
        return switch (ordenar) {
            case SCORE -> Comparator.comparingInt((LeadAvaliado la) -> la.avaliacao().score()).reversed();
            case NOTA -> Comparator.comparingDouble(la -> la.avaliacao().notaGoogle());
            case SEGUIDORES -> Comparator.comparingInt(la -> la.avaliacao().seguidores());
            case NOME -> Comparator.comparing(la -> la.empresa().getNome(), String.CASE_INSENSITIVE_ORDER);
        };
    }

    private List<LeadAvaliado> avaliadosDoJob(Long jobId) {
        List<Long> empresaIds = jobResultadoRepository.findByJobId(jobId).stream()
                .map(JobResultado::getEmpresaId)
                .toList();
        return empresaRepository.findAllById(empresaIds).stream()
                .map(e -> new LeadAvaliado(e, avaliador.avaliar(e)))
                .toList();
    }

    /** Job do utilizador ou exceção (não revela jobs de outros). */
    private JobBusca jobDoUtilizador(Long jobId, Long utilizadorId) {
        return jobService.obter(jobId)
                .filter(j -> utilizadorId.equals(j.getUtilizadorId()))
                .orElseThrow(() -> new BuscaNaoEncontradaException(jobId));
    }

    /** Par empresa + avaliação, reutilizado para lista, filtro e exportação. */
    private record LeadAvaliado(Empresa empresa, AvaliacaoLead avaliacao) {
    }
}
