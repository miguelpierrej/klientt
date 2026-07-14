# Deploy (Railway)

Klientt é um **único serviço** Spring Boot + uma base de dados MySQL. Sem scraper, sem proxies,
sem browser headless — só chamadas HTTP a APIs (Casa dos Dados; opcionalmente BrasilAPI/Claude).

## Passos

1. **Base de dados:** adicionar o plugin **MySQL** ao projeto Railway.
2. **Serviço da app:** ligar este repositório. O Railway deteta o `Dockerfile` e faz o build.
3. **Variáveis de ambiente** (Settings → Variables) — ver tabela abaixo.
4. Deploy. A app corre as migrações Flyway no arranque e fica disponível em `$PORT`.

## Variáveis de ambiente

| Variável | Obrigatória | Significado |
|---|:---:|---|
| `SPRING_DATASOURCE_URL` | ✅ | JDBC do MySQL, ex.: `jdbc:mysql://<host>:<port>/<db>?useSSL=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | ✅ | utilizador MySQL |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | password MySQL |
| `PORT` | (auto) | injetada pelo Railway |
| `DESCOBERTA_FONTE` | — | fonte de descoberta por CNAE: `minhareceita` (grátis, default) ou `casadosdados` |
| `MINHA_RECEITA_ENABLED` | — | `true` (default) — descoberta grátis via minhareceita.org |
| `MINHA_RECEITA_BASE_URL` | — | default `https://minhareceita.org` |
| `CNPJ_ENABLED` | — | `true` para ligar a Casa dos Dados (busca por NOME e/ou `DESCOBERTA_FONTE=casadosdados`) |
| `CNPJ_BASE_URL` | — | ex.: `https://api.casadosdados.com.br` |
| `CNPJ_API_KEY` | — | chave do fornecedor |
| `CNPJ_LIMITE` | — | leads por busca / por "carregar mais" (= página grátis; dev usa 20) |
| `STRIPE_SECRET_KEY` | — | chave Stripe (recomendado RAK `rk_`); vazio = compras desligadas |
| `STRIPE_PUBLISHABLE_KEY` | — | chave publicável (`pk_`) para o Embedded Checkout |
| `STRIPE_WEBHOOK_SECRET` | — | signing secret do webhook (`whsec_`) |
| `STRIPE_PRICE_ID` | — | Price do pacote de créditos (ex.: 3000 leads = R$29,90) |
| `STRIPE_LEADS_PACOTE` | — | leads concedidos por compra (default 3000) |
| `CNAE_LLM_ENABLED` | — | `true` para o fallback nicho→CNAE via Gemini |
| `GEMINI_API_KEY` | — | chave Gemini (se CNAE_LLM_ENABLED) |
| `CNAE_MODEL` | — | modelo Gemini (default `gemini-2.5-flash`) |
| `CONTATO_FALLBACK_ENABLED` | — | gate mestre do enriquecimento de contacto (cadeia CNPJá → BrasilAPI) |
| `CONTATO_FALLBACK_BASE_URL` | — | BrasilAPI (fallback), default `https://brasilapi.com.br` |
| `CONTATO_CNPJA_ENABLED` | — | CNPJá open (primário), default `true` |
| `CONTATO_CNPJA_BASE_URL` | — | default `https://open.cnpja.com` |
| `CONTATO_CNPJA_RPM` | — | teto req/min do CNPJá (default 5) |
| `SCRAPER_ENABLED` | — | `true` para enriquecer via scraper (Novo Fluxo); senão o job conclui na descoberta |
| `SCRAPER_BASE_URL` | — | URL do serviço de scraping (ex.: `http://scraper:8000`) |
| `SCRAPER_TOKEN` | — | token partilhado (header `X-Klientt-Token`); tem de bater com o do scraper |
| `SCRAPER_CALLBACK_BASE_URL` | — | URL pública deste app p/ o callback (ex.: `https://klientt.up.railway.app`) |
| `SCRAPER_USAR_MAPS` / `SCRAPER_VERIFICAR_SMTP` | — | gates opcionais (Maps default on, SMTP default off) |

> **Enriquecimento (Novo Fluxo):** com `SCRAPER_ENABLED=true`, o Klientt é **2 serviços** — este app +
> o scraper (`~/Develop/klientt_scraper`, FastAPI/Playwright). Após a descoberta (Casa dos Dados), a
> lista vai ao scraper (`/v1/enrich`) e volta enriquecida por callback (`/api/scraper/callback`).
> Desligado, o comportamento é o só-API (lista contactável sem presença digital).

> Pagamentos (Stripe/subscrição) estão **desativados** — ver [STRIPE-ASSINATURA.md](./STRIPE-ASSINATURA.md)
> para reativar. Não há variáveis de ambiente Stripe.

> Sem `SPRING_DATASOURCE_*`, a app arranca em **H2 em memória** (perde os dados a cada reinício) —
> útil para um primeiro deploy de teste, não para produção.

## Build/run local

```bash
./mvnw test            # 64 testes
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # H2 + chaves de teste
```
