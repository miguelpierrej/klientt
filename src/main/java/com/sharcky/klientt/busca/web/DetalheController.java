package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.service.LeadDetalheService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DetalheController {

    private final LeadDetalheService detalheService;

    public DetalheController(LeadDetalheService detalheService) {
        this.detalheService = detalheService;
    }

    /** Fragmento com o detalhe completo de um lead (carregado no modal via HTMX). */
    @GetMapping("/empresa/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("lead", detalheService.detalhe(id));
        return "fragments/detalhe :: modal";
    }
}
