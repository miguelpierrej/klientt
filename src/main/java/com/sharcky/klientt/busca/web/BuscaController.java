package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.dto.ResultadoBusca;
import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.service.BuscaService;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BuscaController {

    private final BuscaService buscaService;
    private final ResolvedorCnae resolvedorCnae;

    public BuscaController(BuscaService buscaService, ResolvedorCnae resolvedorCnae) {
        this.buscaService = buscaService;
        this.resolvedorCnae = resolvedorCnae;
    }

    /** Página principal com o formulário de busca. */
    @GetMapping("/")
    public String index() {
        return "busca";
    }

    /**
     * Trata a submissão da busca. Para NICHO sem CNAE confirmado, devolve primeiro o passo de
     * confirmação do CNAE (não gasta saldo). Com o CNAE confirmado (ou para NOME), inicia o job e
     * devolve o fragmento de espera, que faz polling até os resultados chegarem.
     */
    @PostMapping("/buscar")
    public String iniciar(@Valid @ModelAttribute BuscaRequest buscaRequest,
                          BindingResult binding,
                          @RequestParam(required = false) String cnaeOutro,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("mensagens", binding.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList());
            return "fragments/resultados :: erro";
        }

        // CNAE escolhido: o digitado manualmente prevalece sobre o selecionado.
        String cnae = temTexto(cnaeOutro) ? cnaeOutro.trim()
                : (buscaRequest.temCnae() ? buscaRequest.cnae() : null);

        // NICHO sem CNAE confirmado → pedir confirmação primeiro (não inicia a busca).
        if (buscaRequest.tipo() == TipoBusca.NICHO && cnae == null) {
            model.addAttribute("candidatos", resolvedorCnae.candidatos(buscaRequest.termo()));
            model.addAttribute("termo", buscaRequest.termo());
            model.addAttribute("regiao", buscaRequest.regiao());
            return "fragments/resultados :: confirmar-cnae";
        }

        BuscaRequest efetiva = new BuscaRequest(
                buscaRequest.tipo(), buscaRequest.termo(), buscaRequest.regiao(), cnae);
        Long jobId = buscaService.iniciar(efetiva, utilizador.getId());
        model.addAttribute("jobId", jobId);
        return "fragments/resultados :: aguardar";
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Polling do estado do job (chamado pelo HTMX).
     * Devolve: espera (continua a sondar), lista (concluído) ou erro.
     */
    @GetMapping("/buscar/{jobId}")
    public String consultar(@PathVariable Long jobId,
                            @AuthenticationPrincipal KlienttUserDetails utilizador,
                            Model model) {
        ResultadoBusca resultado = buscaService.consultar(jobId, utilizador.getId());

        if (resultado.falhou()) {
            model.addAttribute("mensagens", List.of("A busca falhou. Por favor, tente novamente."));
            return "fragments/resultados :: erro";
        }
        if (resultado.concluido()) {
            model.addAttribute("leads", resultado.leads());
            model.addAttribute("termo", resultado.termo());
            model.addAttribute("jobId", jobId);
            return "fragments/resultados :: lista";
        }
        if (!resultado.leads().isEmpty()) {
            model.addAttribute("leads", resultado.leads());
            model.addAttribute("termo", resultado.termo());
            model.addAttribute("jobId", jobId);
            return "fragments/resultados :: parcial";
        }
        model.addAttribute("jobId", jobId);
        return "fragments/resultados :: aguardar";
    }

    /**
     * Re-renderiza apenas a lista de leads com filtros/ordenação (HTMX), sem repetir a busca.
     */
    @GetMapping("/buscar/{jobId}/resultados")
    public String filtrar(@PathVariable Long jobId,
                          @RequestParam(required = false) OrdenarPor ordenar,
                          @RequestParam(defaultValue = "false") boolean comContato,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        FiltroBusca filtro = new FiltroBusca(ordenar, comContato);
        model.addAttribute("leads", buscaService.filtrar(jobId, utilizador.getId(), filtro));
        return "fragments/resultados :: leads";
    }
}
