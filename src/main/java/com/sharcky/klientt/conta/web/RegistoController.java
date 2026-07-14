package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.dto.RegistoRequest;
import com.sharcky.klientt.conta.email.EmailService;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.service.EmailJaRegistadoException;
import com.sharcky.klientt.conta.service.RegistoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Controller
public class RegistoController {

    private final RegistoService registoService;
    private final EmailService emailService;

    public RegistoController(RegistoService registoService, EmailService emailService) {
        this.registoService = registoService;
        this.emailService = emailService;
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
        Utilizador u;
        try {
            u = registoService.registar(registoRequest);
        } catch (EmailJaRegistadoException ex) {
            binding.rejectValue("email", "emailExiste", ex.getMessage());
            return "registo";
        }
        emailService.enviarVerificacao(u.getEmail(), u.getNome(), linkVerificacao(u.getTokenVerificacao()));
        return "redirect:/verifica-email?email=" + java.net.URLEncoder.encode(u.getEmail(), StandardCharsets.UTF_8);
    }

    /** Monta o link absoluto de confirmação a partir do pedido atual (dev: localhost; prod: domínio real). */
    static String linkVerificacao(String token) {
        return UriComponentsBuilder.fromUriString(
                        ServletUriComponentsBuilder.fromCurrentContextPath().toUriString())
                .path("/verificar-email")
                .queryParam("token", token)
                .toUriString();
    }
}
