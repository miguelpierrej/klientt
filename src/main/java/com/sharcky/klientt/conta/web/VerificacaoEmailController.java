package com.sharcky.klientt.conta.web;

import com.sharcky.klientt.conta.email.EmailService;
import com.sharcky.klientt.conta.service.RegistoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Confirmação de email em dois passos:
 * <ul>
 *   <li>Página pós-registo ({@code /verifica-email}) que faz <b>polling</b> ao {@code /status}
 *       e avança sozinha quando o email é confirmado (noutra aba ou noutro dispositivo).</li>
 *   <li>Link do email ({@code /verificar-email}) que confirma e mostra uma página "pode fechar".</li>
 * </ul>
 */
@Controller
public class VerificacaoEmailController {

    /** Email cuja confirmação a aba atual está a aguardar (permite o polling sem enumerar emails). */
    public static final String SESSION_EMAIL_PENDENTE = "emailVerificacaoPendente";

    private final RegistoService registoService;
    private final EmailService emailService;

    public VerificacaoEmailController(RegistoService registoService, EmailService emailService) {
        this.registoService = registoService;
        this.emailService = emailService;
    }

    /** Página de aguardo (faz polling). O email vem da sessão (definido no registo/reenvio). */
    @GetMapping("/verifica-email")
    public String aguardaConfirmacao(@RequestParam(required = false) String email, HttpSession session, Model model) {
        String pendente = (email != null && !email.isBlank())
                ? email.trim().toLowerCase()
                : (String) session.getAttribute(SESSION_EMAIL_PENDENTE);
        if (pendente != null) {
            session.setAttribute(SESSION_EMAIL_PENDENTE, pendente);
        }
        model.addAttribute("email", pendente);
        return "verifica-email";
    }

    /** Polling: a conta pendente (da sessão) já foi confirmada? Devolve {@code {"verificado": bool}}. */
    @GetMapping("/verifica-email/status")
    @ResponseBody
    public Map<String, Boolean> status(HttpSession session) {
        Object pendente = session.getAttribute(SESSION_EMAIL_PENDENTE);
        boolean verificado = pendente instanceof String email && registoService.emailConfirmado(email);
        return Map.of("verificado", verificado);
    }

    /** Clique no link do email: confirma o token e mostra a página "pode fechar esta aba". */
    @GetMapping("/verificar-email")
    public String confirmar(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("ok", registoService.confirmar(token));
        return "email-confirmado";
    }

    /** Reenvia o email de confirmação (token novo). Não revela se o email existe. */
    @PostMapping("/verificar-email/reenviar")
    public String reenviar(@RequestParam String email, HttpSession session) {
        registoService.prepararReenvio(email).ifPresent(u ->
                emailService.enviarVerificacao(u.getEmail(), u.getNome(),
                        RegistoController.linkVerificacao(u.getTokenVerificacao())));
        session.setAttribute(SESSION_EMAIL_PENDENTE, email.trim().toLowerCase());
        return "redirect:/verifica-email?reenviado";
    }
}
