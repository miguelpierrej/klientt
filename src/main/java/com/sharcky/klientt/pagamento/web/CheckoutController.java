package com.sharcky.klientt.pagamento.web;

import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import com.sharcky.klientt.conta.service.CreditosService;
import com.sharcky.klientt.pagamento.config.StripeProperties;
import com.sharcky.klientt.pagamento.service.PagamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@Controller
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final PagamentoService pagamentoService;
    private final StripeProperties stripe;
    private final CreditosService creditosService;

    public CheckoutController(PagamentoService pagamentoService, StripeProperties stripe,
                              CreditosService creditosService) {
        this.pagamentoService = pagamentoService;
        this.stripe = stripe;
        this.creditosService = creditosService;
    }

    /** Página de compra com o Embedded Checkout. */
    @GetMapping("/creditos")
    public String creditos(@AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        model.addAttribute("disponivel", pagamentoService.disponivel());
        model.addAttribute("publishableKey", stripe.getPublishableKey());
        model.addAttribute("leadsPorPacote", stripe.getLeadsPorPacote());
        model.addAttribute("saldo", creditosService.disponivel(utilizador.getId()));
        return "creditos";
    }

    /** Cria a Checkout Session e devolve o clientSecret (consumido pelo Stripe.js embutido). */
    @GetMapping("/creditos/checkout")
    @ResponseBody
    public Map<String, String> checkout(@AuthenticationPrincipal KlienttUserDetails utilizador) {
        String returnUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/creditos/sucesso").toUriString();
        return Map.of("clientSecret", pagamentoService.criarCheckout(utilizador.getId(), returnUrl));
    }

    /**
     * Regresso do Embedded Checkout. Confirma a sessão diretamente na Stripe (via session_id) e
     * credita os leads — é o caminho principal de fulfillment, não depende do webhook.
     */
    @GetMapping("/creditos/sucesso")
    public String sucesso(@RequestParam(name = "session_id", required = false) String sessionId,
                          @AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        int creditados = 0;
        try {
            creditados = pagamentoService.confirmarPagamento(sessionId, utilizador.getId());
        } catch (RuntimeException ex) {
            // Ex.: corrida com o webhook (UNIQUE session_id). O saldo abaixo reflete o estado real.
            log.warn("Falha a confirmar a sessão {} no regresso: {}", sessionId, ex.getMessage());
        }
        model.addAttribute("creditados", creditados);
        model.addAttribute("saldo", creditosService.disponivel(utilizador.getId()));
        return "creditos-sucesso";
    }
}
