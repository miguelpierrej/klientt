package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.dto.RedefinirSenhaRequest;
import com.sharcky.klientt.conta.email.EmailService;
import com.sharcky.klientt.conta.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Recuperação de password: pedir link ("esqueci-me") e definir nova password a partir do token.
 */
@Controller
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoService;
    private final EmailService emailService;

    public RecuperacaoSenhaController(RecuperacaoSenhaService recuperacaoService, EmailService emailService) {
        this.recuperacaoService = recuperacaoService;
        this.emailService = emailService;
    }

    /** Formulário "esqueci-me da password". */
    @GetMapping("/recuperar-senha")
    public String pedirForm() {
        return "recuperar-senha";
    }

    /** Envia o link de redefinição (se a conta existir). Nunca revela se o email existe. */
    @PostMapping("/recuperar-senha")
    public String pedir(@RequestParam String email) {
        recuperacaoService.prepararRecuperacao(email).ifPresent(u ->
                emailService.enviarRecuperacaoSenha(u.getEmail(), u.getNome(), linkReset(u.getTokenReset())));
        return "redirect:/recuperar-senha?enviado";
    }

    /** Link do email: mostra o formulário de nova password se o token for válido. */
    @GetMapping("/redefinir-senha")
    public String redefinirForm(@RequestParam(required = false) String token, Model model) {
        if (!recuperacaoService.tokenValido(token)) {
            return "redirect:/recuperar-senha?tokenInvalido";
        }
        model.addAttribute("redefinirSenhaRequest", new RedefinirSenhaRequest(token, ""));
        return "redefinir-senha";
    }

    /** Salva a nova senha. */
    @PostMapping("/redefinir-senha")
    public String redefinir(@Valid @ModelAttribute RedefinirSenhaRequest redefinirSenhaRequest,
                            BindingResult binding) {
        if (binding.hasErrors()) {
            return "redefinir-senha";
        }
        boolean ok = recuperacaoService.redefinir(redefinirSenhaRequest.token(), redefinirSenhaRequest.password());
        return ok ? "redirect:/login?senhaRedefinida" : "redirect:/recuperar-senha?tokenInvalido";
    }

    /** Link absoluto de redefinição a partir do pedido atual (dev: localhost; prod: domínio real). */
    private static String linkReset(String token) {
        return UriComponentsBuilder.fromUriString(
                        ServletUriComponentsBuilder.fromCurrentContextPath().toUriString())
                .path("/redefinir-senha")
                .queryParam("token", token)
                .toUriString();
    }
}
