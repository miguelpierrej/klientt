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
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.conta.service.CreditosService;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Orquestra o fluxo assíncrono de busca: cria o job, dispara a descoberta (Casa dos Dados) e, no
 * polling, devolve o estado e os leads. O produto é uma lista contactável — sem score de "dor"
 * (PLANO-SO-API.md, Fase A): ordena por contactável → mais recente → nome.
 */
@Service
public class BuscaServiceImpl implements BuscaService {

    private final JobService jobService;
    private final CreditosService creditosService;
    private final FonteCnpjExecutor fonteCnpjExecutor;
    private final JobResultadoRepository jobResultadoRepository;
    private final EmpresaRepository empresaRepository;
    private final LeadMapper leadMapper;
    private final LeadDetalheMapper detalheMapper;
    private final FonteCnpj fonteCnpj;
    private final IngestaoService ingestaoService;
    private final ClienteCnpjProperties cnpjProperties;

    public BuscaServiceImpl(JobService jobService, CreditosService creditosService,
                            FonteCnpjExecutor fonteCnpjExecutor,
                            JobResultadoRepository jobResultadoRepository,
                            EmpresaRepository empresaRepository,
                            LeadMapper leadMapper, LeadDetalheMapper detalheMapper,
                            FonteCnpj fonteCnpj, IngestaoService ingestaoService,
                            ClienteCnpjProperties cnpjProperties) {
        this.jobService = jobService;
        this.creditosService = creditosService;
        this.fonteCnpjExecutor = fonteCnpjExecutor;
        this.jobResultadoRepository = jobResultadoRepository;
        this.empresaRepository = empresaRepository;
        this.leadMapper = leadMapper;
        this.detalheMapper = detalheMapper;
        this.fonteCnpj = fonteCnpj;
        this.ingestaoService = ingestaoService;
        this.cnpjProperties = cnpjProperties;
    }

    @Override
    public Long iniciar(BuscaRequest request, Long utilizadorId) {
        // A 1ª página é grátis — a busca não exige créditos (o gate está no "carregar mais").
        // criar() é transacional e faz commit antes de dispararmos a descoberta — assim o trabalho
        // assíncrono encontra sempre o job já persistido.
        Long jobId = jobService.criar(request, utilizadorId);
        fonteCnpjExecutor.executar(jobId, request.tipo(), request.termo(), request.regiao(), request.cnae());
        return jobId;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoBusca consultar(Long jobId, Long utilizadorId) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);

        if (job.getEstado() == EstadoJob.ERRO) {
            return new ResultadoBusca(jobId, job.getTermo(), job.getEstado(), List.of(), null);
        }

        List<LeadResponse> leads = empresasDoJob(jobId).stream()
                .sorted(comparador(OrdenarPor.RELEVANCIA))
                .map(leadMapper::toResponse)
                .toList();

        return new ResultadoBusca(jobId, job.getTermo(), job.getEstado(), leads, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadResponse> filtrar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        return aplicar(jobId, utilizadorId, filtro).stream()
                .map(leadMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadDetalhe> exportar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        return aplicar(jobId, utilizadorId, filtro).stream()
                .map(detalheMapper::toDetalhe)
                .toList();
    }

    @Override
    @Transactional
    public void carregarMais(Long jobId, Long utilizadorId) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);
        if (job.getCursor() == null || job.getCursor().isBlank()) {
            return;   // esgotado na fonte
        }
        long saldo = creditosService.disponivel(utilizadorId);
        if (saldo <= 0) {
            return;   // sem créditos → o controlador oferece a compra
        }
        // Só NICHO com CNAE confirmado tem cursor. Não busca mais do que o saldo permite.
        int aBuscar = (int) Math.min(cnpjProperties.getLimiteDefault(), saldo);
        FonteCnpj.Pagina pagina = fonteCnpj.buscarPaginaPorCnae(
                job.getCnae(), job.getRegiao(), job.getCursor(), aBuscar);
        ingestaoService.ingerir(pagina.empresas(), jobId);
        jobService.registarCursor(jobId, pagina.cursor());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temMais(Long jobId, Long utilizadorId) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);
        return job.getCursor() != null && !job.getCursor().isBlank();
    }

    /** Aplica o filtro e a ordenação aos leads de um job concluído (do utilizador). */
    private List<Empresa> aplicar(Long jobId, Long utilizadorId, FiltroBusca filtro) {
        JobBusca job = jobDoUtilizador(jobId, utilizadorId);
        if (job.getEstado() != EstadoJob.CONCLUIDO) {
            return List.of();
        }
        return empresasDoJob(jobId).stream()
                .filter(e -> !filtro.comContato() || e.isContactavel())
                .sorted(comparador(filtro.ordenarOuPadrao()))
                .toList();
    }

    private Comparator<Empresa> comparador(OrdenarPor ordenar) {
        Comparator<Empresa> porRecente =
                Comparator.comparing(Empresa::getDataAbertura, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<Empresa> porNome =
                Comparator.comparing(Empresa::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        return switch (ordenar) {
            case RELEVANCIA -> Comparator.comparing(Empresa::isContactavel).reversed()
                    .thenComparing(porRecente).thenComparing(porNome);
            case RECENTE -> porRecente.thenComparing(porNome);
            case NOME -> porNome;
        };
    }

    private List<Empresa> empresasDoJob(Long jobId) {
        List<Long> empresaIds = jobResultadoRepository.findByJobId(jobId).stream()
                .map(JobResultado::getEmpresaId)
                .toList();
        return empresaRepository.findAllById(empresaIds);
    }

    /** Job do utilizador ou exceção (não revela jobs de outros). */
    private JobBusca jobDoUtilizador(Long jobId, Long utilizadorId) {
        return jobService.obter(jobId)
                .filter(j -> utilizadorId.equals(j.getUtilizadorId()))
                .orElseThrow(() -> new BuscaNaoEncontradaException(jobId));
    }
}
