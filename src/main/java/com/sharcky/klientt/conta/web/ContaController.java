package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import com.sharcky.klientt.conta.service.ContaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/conta")
    public String conta(@AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        model.addAttribute("conta", contaService.resumo(utilizador.getId()));
        return "conta";
    }
}
