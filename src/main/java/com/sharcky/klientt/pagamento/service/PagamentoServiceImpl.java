package com.sharcky.klientt.pagamento.service;

import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import com.sharcky.klientt.pagamento.config.StripeProperties;
import com.sharcky.klientt.pagamento.dto.SubscricaoIntent;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoServiceImpl implements PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoServiceImpl.class);
    private static final String PLANO_GRATIS = "Teste";

    private final StripeProperties stripe;
    private final UtilizadorRepository utilizadorRepository;
    private final PlanoRepository planoRepository;

    public PagamentoServiceImpl(StripeProperties stripe, UtilizadorRepository utilizadorRepository,
                                PlanoRepository planoRepository) {
        this.stripe = stripe;
        this.utilizadorRepository = utilizadorRepository;
        this.planoRepository = planoRepository;
    }

    @Override
    public boolean disponivel() {
        return stripe.isEnabled();
    }

    @Override
    @Transactional
    public SubscricaoIntent iniciarSubscricao(Long utilizadorId, String planoNome) {
        if (!stripe.isEnabled()) {
            throw new PagamentoIndisponivelException("Pagamentos não estão configurados.");
        }
        String priceId = stripe.priceId(planoNome);
        if (priceId == null || priceId.isBlank()) {
            throw new PagamentoIndisponivelException("Plano sem preço Stripe configurado: " + planoNome);
        }
        Utilizador u = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + utilizadorId));

        Stripe.apiKey = stripe.getSecretKey();
        try {
            String customerId = garantirCliente(u);

            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                            .setSaveDefaultPaymentMethod(
                                    SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                            .build())
                    .addExpand("latest_invoice.confirmation_secret")
                    .build();

            Subscription sub = Subscription.create(params);
            u.setStripeSubscriptionId(sub.getId());
            u.setSubscriptionStatus(sub.getStatus());
            utilizadorRepository.save(u);

            Invoice invoice = sub.getLatestInvoiceObject();
            String clientSecret = invoice.getConfirmationSecret().getClientSecret();
            return new SubscricaoIntent(clientSecret, stripe.getPublishableKey(), planoNome);
        } catch (StripeException ex) {
            log.error("Erro Stripe ao criar subscrição (user={}, plano={})", utilizadorId, planoNome, ex);
            throw new PagamentoIndisponivelException("Falha ao iniciar o pagamento. Tente novamente.");
        }
    }

    private String garantirCliente(Utilizador u) throws StripeException {
        if (u.getStripeCustomerId() != null) {
            return u.getStripeCustomerId();
        }
        Customer cliente = Customer.create(CustomerCreateParams.builder()
                .setEmail(u.getEmail())
                .setName(u.getNome())
                .build());
        u.setStripeCustomerId(cliente.getId());
        return cliente.getId();
    }

    @Override
    @Transactional
    public void processarWebhook(String payload, String assinatura) {
        if (!stripe.isEnabled()) {
            return;
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, assinatura, stripe.getWebhookSecret());
        } catch (Exception ex) {
            log.warn("Webhook Stripe inválido: {}", ex.getMessage());
            throw new PagamentoIndisponivelException("Assinatura do webhook inválida.");
        }

        if (event.getType().startsWith("customer.subscription.")) {
            event.getDataObjectDeserializer().getObject()
                    .filter(Subscription.class::isInstance)
                    .map(Subscription.class::cast)
                    .ifPresent(this::aplicarSubscricao);
        }
    }

    private void aplicarSubscricao(Subscription sub) {
        utilizadorRepository.findByStripeCustomerId(sub.getCustomer()).ifPresent(u -> {
            u.setSubscriptionStatus(sub.getStatus());
            u.setStripeSubscriptionId(sub.getId());

            if ("active".equals(sub.getStatus()) || "trialing".equals(sub.getStatus())) {
                String priceId = sub.getItems().getData().get(0).getPrice().getId();
                String plano = stripe.planoDoPrice(priceId);
                if (plano != null) {
                    planoRepository.findByNome(plano).ifPresent(u::setPlano);
                }
            } else if ("canceled".equals(sub.getStatus())) {
                planoRepository.findByNome(PLANO_GRATIS).ifPresent(u::setPlano);
            }
            utilizadorRepository.save(u);
            log.info("Subscrição {} → estado {} (user {})", sub.getId(), sub.getStatus(), u.getId());
        });
    }
}
