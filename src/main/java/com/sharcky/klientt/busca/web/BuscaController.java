package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.dto.PaginaLeads;
import com.sharcky.klientt.busca.dto.ResultadoBusca;
import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.busca.service.BuscaService;
import com.sharcky.klientt.cnae.ResolvedorCnae;
import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import com.sharcky.klientt.conta.service.CreditosService;
import com.sharcky.klientt.perfil.PerfilService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final CreditosService creditosService;
    private final PerfilService perfilService;
    /** Leads por página na lista de resultados (klientt.busca.tamanho-pagina, default 20). */
    private final int tamanhoPagina;

    public BuscaController(BuscaService buscaService, ResolvedorCnae resolvedorCnae,
                           CreditosService creditosService, PerfilService perfilService,
                           @Value("${klientt.busca.tamanho-pagina:20}") int tamanhoPagina) {
        this.buscaService = buscaService;
        this.resolvedorCnae = resolvedorCnae;
        this.creditosService = creditosService;
        this.perfilService = perfilService;
        this.tamanhoPagina = tamanhoPagina;
    }

    /** App: formulário de busca (autenticado). No 1º acesso, redireciona para o onboarding do perfil. */
    @GetMapping("/app")
    public String index(@AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        var perfil = perfilService.obter(utilizador.getId());
        if (perfil.map(p -> !p.isConcluido()).orElse(true)) {
            return "redirect:/onboarding";
        }
        // Atalhos "buscar meus alvos" a partir do perfil (nichos com descrição + regiões).
        model.addAttribute("atalhosNichos", perfilService.nichosDetalhados(perfil.get()));
        model.addAttribute("atalhosRegioes", perfil.get().regioes());
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
            adicionarPagina(model, resultado.leads(), 1);
            model.addAttribute("podeCarregarMais", buscaService.temMais(jobId, utilizador.getId()));
            model.addAttribute("temCreditos", creditosService.temDisponivel(utilizador.getId()));
            model.addAttribute("termo", resultado.termo());
            model.addAttribute("jobId", jobId);
            return "fragments/resultados :: lista";
        }
        if (!resultado.leads().isEmpty()) {
            adicionarPagina(model, resultado.leads(), 1);
            model.addAttribute("streaming", true);   // durante o streaming não mostra paginação
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
                          @RequestParam(defaultValue = "1") int pagina,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        FiltroBusca filtro = new FiltroBusca(ordenar, comContato);
        List<LeadResponse> todos = buscaService.filtrar(jobId, utilizador.getId(), filtro);
        model.addAttribute("jobId", jobId);
        model.addAttribute("ordenar", filtro.ordenarOuPadrao());
        model.addAttribute("comContato", comContato);
        model.addAttribute("pagina", PaginaLeads.de(todos, pagina, tamanhoPagina));
        model.addAttribute("podeCarregarMais", buscaService.temMais(jobId, utilizador.getId()));
        model.addAttribute("temCreditos", creditosService.temDisponivel(utilizador.getId()));
        return "fragments/resultados :: leads";
    }

    /**
     * "Carregar mais": vai buscar a próxima página à fonte (via cursor) e devolve a página seguinte
     * já com os novos resultados.
     */
    @GetMapping("/buscar/{jobId}/mais")
    public String carregarMais(@PathVariable Long jobId,
                               @RequestParam(required = false) OrdenarPor ordenar,
                               @RequestParam(defaultValue = "false") boolean comContato,
                               @RequestParam(defaultValue = "1") int pagina,
                               @AuthenticationPrincipal KlienttUserDetails utilizador,
                               Model model) {
        Long uid = utilizador.getId();
        buscaService.carregarMais(jobId, uid);
        FiltroBusca filtro = new FiltroBusca(ordenar, comContato);
        List<LeadResponse> todos = buscaService.filtrar(jobId, uid, filtro);
        model.addAttribute("jobId", jobId);
        model.addAttribute("ordenar", filtro.ordenarOuPadrao());
        model.addAttribute("comContato", comContato);
        model.addAttribute("pagina", PaginaLeads.de(todos, pagina + 1, tamanhoPagina));   // salta p/ a nova página
        model.addAttribute("podeCarregarMais", buscaService.temMais(jobId, uid));
        model.addAttribute("temCreditos", creditosService.temDisponivel(uid));
        return "fragments/resultados :: leads";
    }

    /** Adiciona ao modelo a página de leads + o estado de filtro (para os links de paginação). */
    private void adicionarPagina(Model model, List<LeadResponse> leads, int pagina) {
        model.addAttribute("pagina", PaginaLeads.de(leads, pagina, tamanhoPagina));
        model.addAttribute("ordenar", OrdenarPor.RELEVANCIA);
        model.addAttribute("comContato", false);
    }
}
