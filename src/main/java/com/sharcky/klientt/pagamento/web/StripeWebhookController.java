package com.sharcky.klientt.pagamento.web;

import com.sharcky.klientt.pagamento.service.PagamentoIndisponivelException;
import com.sharcky.klientt.pagamento.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook da Stripe (eventos de subscrição). Autenticado pela assinatura Stripe-Signature,
 * por isso é público e isento de CSRF (ver SecurityConfig).
 */
@RestController
public class StripeWebhookController {

    private final PagamentoService pagamentoService;

    public StripeWebhookController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader(value = "Stripe-Signature", required = false) String assinatura) {
        try {
            pagamentoService.processarWebhook(payload, assinatura);
            return ResponseEntity.ok("ok");
        } catch (PagamentoIndisponivelException ex) {
            return ResponseEntity.badRequest().body("invalid");
        }
    }
}
