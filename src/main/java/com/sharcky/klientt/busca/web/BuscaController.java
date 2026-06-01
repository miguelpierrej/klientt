package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.dto.ResultadoBusca;
import com.sharcky.klientt.busca.service.BuscaService;
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

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    /** Página principal com o formulário de busca. */
    @GetMapping("/")
    public String index() {
        return "busca";
    }

    /**
     * Inicia a busca (cria job + dispara scraper) e devolve o fragmento de espera,
     * que faz polling até os resultados chegarem.
     */
    @PostMapping("/buscar")
    public String iniciar(@Valid @ModelAttribute BuscaRequest buscaRequest,
                          BindingResult binding,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("mensagens", binding.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList());
            return "fragments/resultados :: erro";
        }

        Long jobId = buscaService.iniciar(buscaRequest, utilizador.getId());
        model.addAttribute("jobId", jobId);
        return "fragments/resultados :: aguardar";
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
        model.addAttribute("jobId", jobId);
        return "fragments/resultados :: aguardar";
    }

    /**
     * Re-renderiza apenas a lista de leads com filtros/ordenação (HTMX), sem repetir a busca.
     */
    @GetMapping("/buscar/{jobId}/resultados")
    public String filtrar(@PathVariable Long jobId,
                          @RequestParam(required = false) OrdenarPor ordenar,
                          @RequestParam(defaultValue = "false") boolean semSite,
                          @RequestParam(defaultValue = "false") boolean notaBaixa,
                          @RequestParam(defaultValue = "false") boolean poucosSeguidores,
                          @RequestParam(defaultValue = "false") boolean procon,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        FiltroBusca filtro = new FiltroBusca(ordenar, semSite, notaBaixa, poucosSeguidores, procon);
        model.addAttribute("leads", buscaService.filtrar(jobId, utilizador.getId(), filtro));
        return "fragments/resultados :: leads";
    }
}
