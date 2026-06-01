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
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.busca.scoring.AvaliadorLead;
import com.sharcky.klientt.conta.service.QuotaExcedidaException;
import com.sharcky.klientt.conta.service.QuotaService;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;

import java.util.List;
import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuscaServiceImplTest {

    @Mock JobService jobService;
    @Mock QuotaService quotaService;
    @Mock ScraperClient scraperClient;
    @Mock ScraperProperties properties;
    @Mock JobResultadoRepository jobResultadoRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock AvaliadorLead avaliador;
    @Mock LeadMapper leadMapper;
    @InjectMocks BuscaServiceImpl buscaService;

    private final BuscaRequest request = new BuscaRequest(TipoBusca.NICHO, "bares", "Lisboa");

    @Test
    void iniciarValidaQuotaCriaJobEDisparaScraper() {
        when(properties.getLimiteDefault()).thenReturn(50);
        when(properties.callbackUrl()).thenReturn("http://cb");
        when(jobService.criar(request, 1L)).thenReturn(7L);

        Long jobId = buscaService.iniciar(request, 1L);

        assertThat(jobId).isEqualTo(7L);
        InOrder inOrder = inOrder(quotaService, jobService, scraperClient);
        inOrder.verify(quotaService).garantirDisponibilidade(1L);
        inOrder.verify(jobService).criar(request, 1L);
        inOrder.verify(scraperClient).iniciarBusca(any());
    }

    @Test
    void iniciarComQuotaEsgotadaNaoCriaJob() {
        doThrow(new QuotaExcedidaException(20)).when(quotaService).garantirDisponibilidade(1L);

        assertThatThrownBy(() -> buscaService.iniciar(request, 1L))
                .isInstanceOf(QuotaExcedidaException.class);

        verify(jobService, never()).criar(any(), any());
        verify(scraperClient, never()).iniciarBusca(any());
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
    void consultarProprioJobAindaAProcessarDevolveEstadoSemLeads() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setTermo("bares");
        job.setEstado(EstadoJob.A_PROCESSAR);
        when(jobService.obter(5L)).thenReturn(Optional.of(job));

        ResultadoBusca r = buscaService.consultar(5L, 1L);

        assertThat(r.estado()).isEqualTo(EstadoJob.A_PROCESSAR);
        assertThat(r.leads()).isEmpty();
        verify(jobResultadoRepository, never()).findByJobId(any());
    }

    @Test
    void filtrarSemSiteDevolveApenasSemSite() {
        prepararDoisLeads();

        List<LeadResponse> leads = buscaService.filtrar(5L, 1L,
                new FiltroBusca(null, true, false, false, false));

        assertThat(leads).extracting(LeadResponse::nome).containsExactly("Alfa");
    }

    @Test
    void filtrarOrdenarPorNome() {
        prepararDoisLeads();

        List<LeadResponse> leads = buscaService.filtrar(5L, 1L,
                new FiltroBusca(OrdenarPor.NOME, false, false, false, false));

        assertThat(leads).extracting(LeadResponse::nome).containsExactly("Alfa", "Beta");
    }

    /** Job concluído do utilizador 1 com 2 leads: "Alfa" (sem site) e "Beta" (com site). */
    private void prepararDoisLeads() {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(1L);
        job.setEstado(EstadoJob.CONCLUIDO);
        when(jobService.obter(5L)).thenReturn(Optional.of(job));

        Empresa e10 = new Empresa();
        e10.setId(10L);
        Empresa e11 = new Empresa();
        e11.setId(11L);
        when(jobResultadoRepository.findByJobId(5L))
                .thenReturn(List.of(new JobResultado(5L, 10L, 60), new JobResultado(5L, 11L, 0)));
        when(empresaRepository.findAllById(any())).thenReturn(List.of(e10, e11));

        when(avaliador.avaliar(any())).thenReturn(new AvaliacaoLead(0, false, false, 0, false, 0));
        LeadResponse alfa = new LeadResponse(10L, "Alfa", "Lx", 3.0, false, false, 100, false, 60);
        LeadResponse beta = new LeadResponse(11L, "Beta", "Lx", 4.5, true, false, 9000, false, 0);
        when(leadMapper.toResponse(eq(e10), any())).thenReturn(alfa);
        when(leadMapper.toResponse(eq(e11), any())).thenReturn(beta);
    }
}
