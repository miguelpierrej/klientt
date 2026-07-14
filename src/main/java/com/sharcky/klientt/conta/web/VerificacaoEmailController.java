package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.email.EmailService;
import com.sharcky.klientt.conta.service.RegistoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

/**
 * Confirmação de email em dois passos: página pós-registo, clique no link e reenvio.
 */
@Controller
public class VerificacaoEmailController {

    private final RegistoService registoService;
    private final EmailService emailService;

    public VerificacaoEmailController(RegistoService registoService, EmailService emailService) {
        this.registoService = registoService;
        this.emailService = emailService;
    }

    /** Página mostrada após o registo: "enviámos um email para X". */
    @GetMapping("/verifica-email")
    public String aguardaConfirmacao(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verifica-email";
    }

    /** Clique no link do email: confirma o token. */
    @GetMapping("/verificar-email")
    public String confirmar(@RequestParam(required = false) String token) {
        return registoService.confirmar(token)
                ? "redirect:/login?verificado"
                : "redirect:/login?tokenInvalido";
    }

    /** Reenvia o email de confirmação (token novo). Não revela se o email existe. */
    @PostMapping("/verificar-email/reenviar")
    public String reenviar(@RequestParam String email) {
        registoService.prepararReenvio(email).ifPresent(u ->
                emailService.enviarVerificacao(u.getEmail(), u.getNome(),
                        RegistoController.linkVerificacao(u.getTokenVerificacao())));
        return "redirect:/verifica-email?reenviado&email="
                + java.net.URLEncoder.encode(email, StandardCharsets.UTF_8);
    }
}
