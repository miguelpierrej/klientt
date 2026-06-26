package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.service.BuscaService;
import com.sharcky.klientt.cnae.Cnae;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuscaControllerTest {

    @Mock BuscaService buscaService;
    @Mock ResolvedorCnae resolvedorCnae;
    @Mock KlienttUserDetails utilizador;

    BuscaController controller() {
        return new BuscaController(buscaService, resolvedorCnae);
    }

    @Test
    void nichoSemCnaePedeConfirmacaoSemIniciarBusca() {
        BuscaRequest req = new BuscaRequest(TipoBusca.NICHO, "assistência técnica", "São Paulo");
        when(resolvedorCnae.candidatos("assistência técnica"))
                .thenReturn(List.of(new Cnae("9511800", "Reparação de computadores")));
        Model model = new ConcurrentModel();

        String view = controller().iniciar(req, binding(req), null, utilizador, model);

        assertThat(view).isEqualTo("fragments/resultados :: confirmar-cnae");
        assertThat(model.getAttribute("candidatos")).isNotNull();
        verify(buscaService, never()).iniciar(any(), any());
    }

    @Test
    void nichoComCnaeConfirmadoIniciaBusca() {
        BuscaRequest req = new BuscaRequest(TipoBusca.NICHO, "assistência técnica", "São Paulo", "9511800");
        when(utilizador.getId()).thenReturn(1L);
        when(buscaService.iniciar(any(), eq(1L))).thenReturn(7L);
        Model model = new ConcurrentModel();

        String view = controller().iniciar(req, binding(req), null, utilizador, model);

        assertThat(view).isEqualTo("fragments/resultados :: aguardar");
        ArgumentCaptor<BuscaRequest> captor = ArgumentCaptor.forClass(BuscaRequest.class);
        verify(buscaService).iniciar(captor.capture(), eq(1L));
        assertThat(captor.getValue().cnae()).isEqualTo("9511800");
    }

    @Test
    void cnaeOutroPrevaleceSobreOSelecionado() {
        BuscaRequest req = new BuscaRequest(TipoBusca.NICHO, "x", null, "1111111");
        when(utilizador.getId()).thenReturn(1L);
        when(buscaService.iniciar(any(), eq(1L))).thenReturn(7L);

        controller().iniciar(req, binding(req), "2222222", utilizador, new ConcurrentModel());

        ArgumentCaptor<BuscaRequest> captor = ArgumentCaptor.forClass(BuscaRequest.class);
        verify(buscaService).iniciar(captor.capture(), eq(1L));
        assertThat(captor.getValue().cnae()).isEqualTo("2222222");   // o digitado manualmente
    }

    @Test
    void nomeIniciaBuscaDiretamente() {
        BuscaRequest req = new BuscaRequest(TipoBusca.NOME, "Barbearia do Zé", "São Paulo");
        when(utilizador.getId()).thenReturn(1L);
        when(buscaService.iniciar(any(), eq(1L))).thenReturn(7L);
        Model model = new ConcurrentModel();

        String view = controller().iniciar(req, binding(req), null, utilizador, model);

        assertThat(view).isEqualTo("fragments/resultados :: aguardar");
        verify(resolvedorCnae, never()).candidatos(any());
    }

    private BindingResult binding(BuscaRequest req) {
        return new BeanPropertyBindingResult(req, "buscaRequest");   // sem erros
    }
}
