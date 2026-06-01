package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.service.EmailJaRegistadoException;
import com.sharcky.klientt.conta.service.RegistoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistoController {

    private final RegistoService registoService;

    public RegistoController(RegistoService registoService) {
        this.registoService = registoService;
    }

    @GetMapping("/registo")
    public String form(Model model) {
        if (!model.containsAttribute("registoRequest")) {
            model.addAttribute("registoRequest", new RegistoRequest("", "", ""));
        }
        return "registo";
    }

    @PostMapping("/registo")
    public String registar(@Valid @ModelAttribute RegistoRequest registoRequest,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "registo";
        }
        try {
            registoService.registar(registoRequest);
        } catch (EmailJaRegistadoException ex) {
            binding.rejectValue("email", "emailExiste", ex.getMessage());
            return "registo";
        }
        return "redirect:/login?registado";
    }
}
