package com.sharcky.klientt.perfil;

import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Onboarding do perfil do cliente (ICP): captura o que ele vende e quem quer alcançar.
 * Mostrado no 1º acesso (com opção de pular) e reutilizado para editar em "Minha conta".
 */
@Controller
public class OnboardingController {

    /** Opções de porte-alvo (valor guardado → rótulo). */
    static final Map<String, String> PORTES = new LinkedHashMap<>();

    static {
        PORTES.put("MEI", "MEI");
        PORTES.put("MICRO", "Microempresa");
        PORTES.put("PEQUENA", "Pequena empresa");
        PORTES.put("GRANDE", "Média / grande empresa");
    }

    private final PerfilService perfilService;

    public OnboardingController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/onboarding")
    public String form(@AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        PerfilCliente p = perfilService.obter(utilizador.getId()).orElseGet(PerfilCliente::new);
        model.addAttribute("oferta", p.getOferta());
        model.addAttribute("nichosPrefill", perfilService.nichosDetalhados(p));
        model.addAttribute("regioesPrefill", p.regioes());
        model.addAttribute("portesSelecionados", p.portes());
        model.addAttribute("portes", PORTES);
        model.addAttribute("querSemSite", p.isQuerSemSite());
        model.addAttribute("querSimplesMei", p.isQuerSimplesMei());
        model.addAttribute("querComContato", p.isQuerComContato());
        model.addAttribute("editar", p.isConcluido());   // já concluído → veio editar (não é o 1º acesso)
        return "onboarding";
    }

    @PostMapping("/onboarding")
    public String salvar(@AuthenticationPrincipal KlienttUserDetails utilizador,
                         @ModelAttribute PerfilForm perfilForm) {
        perfilService.salvar(utilizador.getId(), perfilForm);
        return "redirect:/app";
    }

    @PostMapping("/onboarding/pular")
    public String pular(@AuthenticationPrincipal KlienttUserDetails utilizador) {
        perfilService.pular(utilizador.getId());
        return "redirect:/app";
    }
}
