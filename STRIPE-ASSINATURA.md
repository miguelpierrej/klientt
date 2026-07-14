# Stripe / Subscrição — desativado

> **Estado:** o módulo de **pagamentos (Stripe) e subscrição de planos** foi **removido** do Klientt.
> Este documento regista o que existia e **como reintroduzir** quando for preciso cobrar.
> Data da remoção: 2026-07-10. Referência de código completo: qualquer commit **até `49ae7f5`**
> (o merge da branch `feat/so-api-fase-a`) contém a implementação funcional — `git show`/`git checkout`
> desses caminhos restaura tudo.

## Porquê
O produto está em fase de validação (beta com agências, ver `ROADMAP.md`). Cobrança não é necessária
para validar utilidade/preço e o Stripe adiciona superfície (chaves, webhook, `permitAll`) e uma
dependência. Desligou-se para simplificar; **planos e quota continuam a funcionar** (todos os
utilizadores ficam no plano gratuito **"Teste"**, atribuído no registo).

## O que continua a existir (NÃO foi removido)
- **Entidade `Plano`** e tabela `planos` (migração `V2__seed_planos.sql` — Teste/Starter/Pro/Agency).
- **Quota mensal** de leads por plano (`QuotaService`, página `/conta` com barra de consumo).
- Atribuição do plano **"Teste"** no registo (`RegistoServiceImpl.PLANO_INICIAL`).
- **Colunas dormentes** em `utilizadores`: `stripe_customer_id`, `stripe_subscription_id`,
  `subscription_status` (migração `V3__stripe.sql` **mantida** — forward-only). Ficam sem uso; não
  foram dropadas para evitar migração destrutiva e facilitar o retorno.

## O que foi removido

### Código Java — package inteiro `com.sharcky.klientt.pagamento`
| Ficheiro | Papel |
|---|---|
| `pagamento/web/PagamentoController.java` | rotas `GET /planos`, `GET /assinar/{plano}`, `GET /pagamento/sucesso` |
| `pagamento/web/StripeWebhookController.java` | `POST /api/stripe/webhook` |
| `pagamento/service/PagamentoService.java` (interface) | `disponivel()`, `iniciarSubscricao()`, `processarWebhook()` |
| `pagamento/service/PagamentoServiceImpl.java` | Stripe SDK: cria Customer + Subscription (DEFAULT_INCOMPLETE), aplica webhook → muda plano |
| `pagamento/service/PagamentoIndisponivelException.java` | erro quando Stripe off / plano sem price |
| `pagamento/config/StripeProperties.java` | `@ConfigurationProperties("klientt.stripe")`; `isEnabled()` = secret começa por `sk_` |
| `pagamento/dto/SubscricaoIntent.java` | record `(clientSecret, publishableKey, planoNome)` |

### Templates Thymeleaf (removidos)
- `templates/planos.html` — grelha de planos + botão "Assinar".
- `templates/assinar.html` — Stripe.js + Payment Element (`https://js.stripe.com/v3/`).
- `templates/pagamento-sucesso.html`.

### Edições (não-remoções)
- **`Utilizador.java`** — removidos os 3 campos `stripeCustomerId` / `stripeSubscriptionId` /
  `subscriptionStatus` (as **colunas** ficam na BD, ver acima).
- **`UtilizadorRepository.java`** — removido `findByStripeCustomerId(...)`.
- **`SecurityConfig.java`** — removido `/api/stripe/**` do `permitAll()` e do `csrf().ignoringRequestMatchers(...)`.
- **`templates/busca.html`** e **`templates/conta.html`** — removido o link de nav `Planos` (`/planos`).
  Em `conta.html` a mensagem de quota cheia deixou de sugerir "upgrade".
- **`pom.xml`** — removida a dependência `com.stripe:stripe-java` e a propriedade `<stripe.version>29.2.0>`.
- **`application.properties`** e **`application-dev.properties`** — removido o bloco `klientt.stripe.*`
  (incl. as chaves de **TESTE** que viviam no perfil `dev`).
- **`DEPLOY.md`** — removidas as linhas das env vars `STRIPE_*`.

## Como reintroduzir

1. **Restaurar o código** a partir do histórico (mais simples que reescrever):
   ```bash
   git checkout 49ae7f5 -- \
     src/main/java/com/sharcky/klientt/pagamento \
     src/main/resources/templates/planos.html \
     src/main/resources/templates/assinar.html \
     src/main/resources/templates/pagamento-sucesso.html
   ```
2. **`pom.xml`** — repor a propriedade `<stripe.version>29.2.0</stripe.version>` e a dependência:
   ```xml
   <dependency>
       <groupId>com.stripe</groupId>
       <artifactId>stripe-java</artifactId>
       <version>${stripe.version}</version>
   </dependency>
   ```
3. **`Utilizador.java`** — repor os campos:
   ```java
   @Column(name = "stripe_customer_id", length = 64)     private String stripeCustomerId;
   @Column(name = "stripe_subscription_id", length = 64) private String stripeSubscriptionId;
   @Column(name = "subscription_status", length = 40)    private String subscriptionStatus;
   ```
   As colunas já existem na BD (V3), portanto **não é preciso nova migração** salvo se, entretanto,
   tiverem sido dropadas.
4. **`UtilizadorRepository.java`** — repor `Optional<Utilizador> findByStripeCustomerId(String id);`.
5. **`SecurityConfig.java`** — repor `/api/stripe/**` em `permitAll()` **e** em
   `csrf().ignoringRequestMatchers("/api/stripe/**")` (o webhook é externo, sem sessão).
6. **Config** — repor o bloco `klientt.stripe.*` em `application.properties` (env vars vazias =
   modo desligado) e as chaves de teste em `application-dev.properties`. Chaves necessárias:
   `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET`,
   `STRIPE_PRICE_STARTER/PRO/AGENCY`.
7. **Nav** — repor o link `Planos` (`/planos`) em `busca.html` e `conta.html`.
8. **DEPLOY.md** — repor as env vars `STRIPE_*` na tabela.
9. **Webhook local (teste):** `stripe listen --forward-to localhost:8080/api/stripe/webhook`.

## Notas de negócio
- Modelo de preços e escalões: ver `~/Documentos/Seniorito/Klientt/PRECOS.md` (Teste/Starter/Pro/Agency).
- Docs no vault (`PRECOS.md`, `ROADMAP.md`, `ARQUITETURA.md`) ainda descrevem o Stripe como
  implementado — estão **desatualizados** quanto a este ponto até se decidir reativar.
