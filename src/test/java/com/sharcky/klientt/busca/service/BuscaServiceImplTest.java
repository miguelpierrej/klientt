package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.dto.ResultadoBusca;
import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.job.EstadoJob;
import com.sharcky.klientt.busca.job.JobBusca;
import com.sharcky.klientt.busca.job.JobResultado;
import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.LeadMapper;
import com.sharcky.klientt.cnpj.FonteCnpj;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.conta.service.CreditosService;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuscaServiceImplTest {

    @Mock JobService jobService;
    @Mock CreditosService creditosService;
    @Mock FonteCnpjExecutor fonteCnpjExecutor;
    @Mock JobResultadoRepository jobResultadoRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock LeadMapper leadMapper;
    @Mock com.sharcky.klientt.busca.mapper.LeadDetalheMapper detalheMapper;
    @Mock FonteCnpj fonteCnpj;
    @Mock IngestaoService ingestaoService;
    @Mock ClienteCnpjProperties cnpjProperties;
    @Mock com.sharcky.klientt.perfil.PerfilService perfilService;
    @Mock RelevanciaService relevanciaService;
    @InjectMocks BuscaServiceImpl buscaService;

    private final BuscaRequest request = new BuscaRequest(TipoBusca.NICHO, "bares", "Lisboa");

    @Test
    void iniciarCriaJobEDisparaFonteCnpjSemGastarCreditos() {
        when(jobService.criar(request, 1L)).thenReturn(7L);

        Long jobId = buscaService.iniciar(request, 1L);

        assertThat(jobId).isEqualTo(7L);
        // A 1ª página é grátis: iniciar não consulta créditos.
        verifyNoInteractions(creditosService);
        InOrder inOrder = inOrder(jobService, fonteCnpjExecutor);
        inOrder.verify(jobService).criar(request, 1L);
        inOrder.verify(fonteCnpjExecutor).executar(7L, TipoBusca.NICHO, "bares", "Lisboa", null);
    }

    @Test
    void carregarMaisSemCreditosNaoBusca() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setCnae("5611201");
        job.setCursor("c1");
        when(jobService.obter(5L)).thenReturn(Optional.of(job));
        when(creditosService.disponivel(1L)).thenReturn(0L);   // sem créditos

        buscaService.carregarMais(5L, 1L);

        verify(fonteCnpj, never()).buscarPaginaPorCnae(any(), any(), any(), anyInt());
        verify(ingestaoService, never()).ingerir(any(), any());
    }

    @Test
    void consultarJobDeOutroUtilizadorDaNaoEncontrada() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(99L);
        job.setEstado(EstadoJob.CONCLUIDO);
        when(jobService.obter(5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> buscaService.consultar(5L, 1L))
                .isInstanceOf(BuscaNaoEncontradaException.class);
    }

    @Test
    void consultarJobInexistenteDaNaoEncontrada() {
        when(jobService.obter(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buscaService.consultar(5L, 1L))
                .isInstanceOf(BuscaNaoEncontradaException.class);
    }

    @Test
    void consultarProprioJobAindaAProcessarDevolveResultadosParciais() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setTermo("bares");
        job.setEstado(EstadoJob.A_PROCESSAR);
        when(jobService.obter(5L)).thenReturn(Optional.of(job));
        when(jobResultadoRepository.findByJobId(5L)).thenReturn(List.of());
        when(empresaRepository.findAllById(List.of())).thenReturn(List.of());

        ResultadoBusca r = buscaService.consultar(5L, 1L);

        assertThat(r.estado()).isEqualTo(EstadoJob.A_PROCESSAR);
        assertThat(r.leads()).isEmpty();
        verify(jobResultadoRepository).findByJobId(5L);
    }

    @Test
    void filtrarOrdenarPorNome() {
        prepararDoisLeads();

        List<LeadResponse> leads = buscaService.filtrar(5L, 1L,
                new FiltroBusca(OrdenarPor.NOME, false));

        assertThat(leads).extracting(LeadResponse::nome).containsExactly("Alfa", "Beta");
    }

    @Test
    void filtrarComContatoDevolveApenasContactaveis() {
        prepararDoisLeads();   // só "Alfa" tem contato

        List<LeadResponse> leads = buscaService.filtrar(5L, 1L,
                new FiltroBusca(null, true));

        assertThat(leads).extracting(LeadResponse::nome).containsExactly("Alfa");
    }

    @Test
    void carregarMaisBuscaProximaPaginaEAtualizaCursor() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setCnae("5611201");
        job.setRegiao("Bauru/SP");
        job.setCursor("c1");
        when(jobService.obter(5L)).thenReturn(Optional.of(job));
        when(creditosService.disponivel(1L)).thenReturn(100L);   // tem créditos
        when(cnpjProperties.getLimiteDefault()).thenReturn(40);
        List<EmpresaPayload> novas = List.of();
        when(fonteCnpj.buscarPaginaPorCnae("5611201", "Bauru/SP", "c1", 40))   // min(40, 100)
                .thenReturn(new FonteCnpj.Pagina(novas, "c2"));

        buscaService.carregarMais(5L, 1L);

        verify(ingestaoService).ingerir(novas, 5L);
        verify(jobService).registarCursor(5L, "c2");
    }

    @Test
    void carregarMaisSemCursorNaoFazNada() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);   // cursor null → esgotado
        when(jobService.obter(5L)).thenReturn(Optional.of(job));

        buscaService.carregarMais(5L, 1L);

        verify(fonteCnpj, never()).buscarPaginaPorCnae(any(), any(), any(), anyInt());
        verify(ingestaoService, never()).ingerir(any(), any());
    }

    @Test
    void temMaisReflecteOCursor() {
        JobBusca comCursor = new JobBusca();
        comCursor.setUtilizadorId(1L);
        comCursor.setCursor("c1");
        when(jobService.obter(5L)).thenReturn(Optional.of(comCursor));
        assertThat(buscaService.temMais(5L, 1L)).isTrue();

        JobBusca semCursor = new JobBusca();
        semCursor.setUtilizadorId(1L);
        when(jobService.obter(6L)).thenReturn(Optional.of(semCursor));
        assertThat(buscaService.temMais(6L, 1L)).isFalse();
    }

    /** Job concluído do utilizador 1 com 2 leads: "Alfa" (contactável) e "Beta" (sem contato). */
    private void prepararDoisLeads() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setEstado(EstadoJob.CONCLUIDO);
        when(jobService.obter(5L)).thenReturn(Optional.of(job));

        Empresa e10 = new Empresa();
        e10.setId(10L);
        e10.setNome("Alfa");
        Contato contato = new Contato();
        contato.setTipo("telefone");
        contato.setValor("+351910000000");
        e10.adicionarContato(contato);   // só Alfa é contactável
        Empresa e11 = new Empresa();
        e11.setId(11L);
        e11.setNome("Beta");
        when(jobResultadoRepository.findByJobId(5L))
                .thenReturn(List.of(new JobResultado(5L, 10L), new JobResultado(5L, 11L)));
        when(empresaRepository.findAllById(any())).thenReturn(List.of(e10, e11));

        LeadResponse alfa = new LeadResponse(10L, "Alfa", "Lx", null, "+351910000000", null, null, true, null, null);
        LeadResponse beta = new LeadResponse(11L, "Beta", "Lx", null, null, null, null, false, null, null);
        lenient().when(leadMapper.toResponse(e10)).thenReturn(alfa);
        lenient().when(leadMapper.toResponse(e11)).thenReturn(beta);
    }
}
