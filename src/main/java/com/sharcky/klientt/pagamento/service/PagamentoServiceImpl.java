package com.sharcky.klientt.pagamento.service;

import com.sharcky.klientt.conta.service.CreditosService;
import com.sharcky.klientt.pagamento.config.StripeProperties;
import com.sharcky.klientt.pagamento.model.Pagamento;
import com.sharcky.klientt.pagamento.repository.PagamentoRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoServiceImpl implements PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoServiceImpl.class);

    private final StripeProperties stripe;
    private final PagamentoRepository pagamentoRepository;
    private final CreditosService creditosService;

    public PagamentoServiceImpl(StripeProperties stripe, PagamentoRepository pagamentoRepository,
                                CreditosService creditosService) {
        this.stripe = stripe;
        this.pagamentoRepository = pagamentoRepository;
        this.creditosService = creditosService;
    }

    @Override
    public boolean disponivel() {
        return stripe.isEnabled() && stripe.getPriceId() != null && !stripe.getPriceId().isBlank();
    }

    @Override
    public String criarCheckout(Long utilizadorId, String returnUrlBase) {
        if (!disponivel()) {
            throw new PagamentoIndisponivelException("Pagamentos não estão configurados.");
        }
        Stripe.apiKey = stripe.getSecretKey();
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)          // pagamento único
                    .setUiMode(SessionCreateParams.UiMode.EMBEDDED)     // UI Stripe no nosso site
                    .setReturnUrl(returnUrlBase + "?session_id={CHECKOUT_SESSION_ID}")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(stripe.getPriceId())
                            .setQuantity(1L)
                            .build())
                    // NÃO definir payment_method_types → métodos dinâmicos (cartão, Pix…).
                    .putMetadata("utilizadorId", String.valueOf(utilizadorId))
                    .putMetadata("leads", String.valueOf(stripe.getLeadsPorPacote()))
                    .build();
            return Session.create(params).getClientSecret();
        } catch (StripeException ex) {
            log.error("Erro Stripe ao criar checkout (user={})", utilizadorId, ex);
            throw new PagamentoIndisponivelException("Falha ao iniciar o pagamento. Tente novamente.");
        }
    }

    @Override
    @Transactional
    public void processarWebhook(String payload, String assinatura) {
        if (!stripe.isEnabled()) {
            return;
        }
        if (assinatura == null || assinatura.isBlank()) {
            // Pedido sem cabeçalho Stripe-Signature → não veio da Stripe (ex.: teste manual/curl).
            log.warn("Pedido a /api/stripe/webhook sem cabeçalho Stripe-Signature — ignorado (não é da Stripe).");
            throw new PagamentoIndisponivelException("Sem assinatura Stripe.");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, assinatura, stripe.getWebhookSecret());
        } catch (Exception ex) {
            log.warn("Webhook Stripe inválido (assinatura não confere com o STRIPE_WEBHOOK_SECRET?): {}", ex.getMessage());
            throw new PagamentoIndisponivelException("Assinatura do webhook inválida.");
        }

        String tipo = event.getType();
        log.info("Webhook Stripe recebido: {} ({})", tipo, event.getId());
        if (!"checkout.session.completed".equals(tipo) && !"checkout.session.async_payment_succeeded".equals(tipo)) {
            return;   // ignora outros eventos
        }

        // getObject() devolve vazio quando a versão de API da conta difere da do SDK — nesse caso,
        // desserializa "unsafe" (best-effort). Sem isto, o crédito falha em silêncio (200 sem creditar).
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject obj;
        if (deserializer.getObject().isPresent()) {
            obj = deserializer.getObject().get();
        } else {
            try {
                obj = deserializer.deserializeUnsafe();
            } catch (Exception ex) {
                log.warn("Não foi possível desserializar o evento {} ({}): {}", tipo, event.getId(), ex.getMessage());
                return;
            }
        }

        if (obj instanceof Session session && "paid".equals(session.getPaymentStatus())) {
            creditar(session);
        } else {
            String status = obj instanceof Session s ? s.getPaymentStatus() : "n/a";
            log.info("Webhook {} sem crédito (payment_status={})", tipo, status);
        }
    }

    @Override
    @Transactional
    public int confirmarPagamento(String sessionId, Long utilizadorId) {
        if (!stripe.isEnabled() || sessionId == null || sessionId.isBlank()) {
            return 0;
        }
        Stripe.apiKey = stripe.getSecretKey();
        Session session;
        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException ex) {
            log.warn("Não foi possível obter a sessão Stripe {}: {}", sessionId, ex.getMessage());
            return 0;
        }
        if (!"paid".equals(session.getPaymentStatus())) {
            log.info("Sessão {} ainda não paga (payment_status={}) — sem crédito", sessionId, session.getPaymentStatus());
            return 0;
        }
        // Segurança: a sessão tem de pertencer ao utilizador autenticado.
        java.util.Map<String, String> meta = session.getMetadata();
        Long dono = parseLong(meta == null ? null : meta.get("utilizadorId"));
        if (dono == null || !dono.equals(utilizadorId)) {
            log.warn("Sessão {} não pertence ao utilizador {} (dono={}) — ignorada", sessionId, utilizadorId, dono);
            return 0;
        }
        return creditar(session);
    }

    /**
     * Credita os leads uma só vez (idempotente por stripe_session_id). Devolve os leads creditados
     * nesta chamada (0 se já tinha sido processada ou sem metadata).
     */
    private int creditar(Session session) {
        String sessionId = session.getId();
        if (pagamentoRepository.existsByStripeSessionId(sessionId)) {
            return 0;   // já processado (webhook + regresso, ou reentrega)
        }
        java.util.Map<String, String> meta = session.getMetadata();
        Long utilizadorId = parseLong(meta == null ? null : meta.get("utilizadorId"));
        if (utilizadorId == null) {
            // Ex.: evento sintético (stripe trigger) sem a nossa metadata → ignora sem crashar.
            log.warn("Checkout pago sem metadata utilizadorId (session={}) — ignorado", sessionId);
            return 0;
        }
        int leads = parseInt(meta.getOrDefault("leads", "0"));

        Pagamento p = new Pagamento();
        p.setUtilizadorId(utilizadorId);
        p.setStripeSessionId(sessionId);
        p.setLeads(leads);
        p.setValorCentavos(session.getAmountTotal() != null ? session.getAmountTotal().intValue() : null);
        p.setMoeda(session.getCurrency());
        p.setEstado("pago");
        pagamentoRepository.save(p);          // UNIQUE(session_id) é o backstop contra corridas
        creditosService.creditar(utilizadorId, leads);
        log.info("Créditos aplicados: user={} +{} leads (session={})", utilizadorId, leads, sessionId);
        return leads;
    }

    private static Long parseLong(String s) {
        try {
            return s == null || s.isBlank() ? null : Long.valueOf(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int parseInt(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
