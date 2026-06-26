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
| `CNPJ_ENABLED` | — | `true` para ligar a descoberta (Casa dos Dados) |
| `CNPJ_BASE_URL` | — | ex.: `https://api.casadosdados.com.br` |
| `CNPJ_API_KEY` | — | chave do fornecedor |
| `CNPJ_LIMITE` | — | limite de empresas por busca (default 25) |
| `CNAE_LLM_ENABLED` | — | `true` para o fallback nicho→CNAE via Claude |
| `ANTHROPIC_API_KEY` | — | chave Anthropic (se CNAE_LLM_ENABLED) |
| `CNAE_MODEL` | — | modelo Claude (default `claude-opus-4-8`) |
| `CONTATO_FALLBACK_ENABLED` | — | `true` para o fallback de contacto por CNPJ (BrasilAPI) |
| `CONTATO_FALLBACK_BASE_URL` | — | default `https://brasilapi.com.br` |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLISHABLE_KEY` / `STRIPE_WEBHOOK_SECRET` | — | pagamentos (vazio = desligado) |
| `STRIPE_PRICE_STARTER` / `STRIPE_PRICE_PRO` / `STRIPE_PRICE_AGENCY` | — | price IDs por plano |

> Sem `SPRING_DATASOURCE_*`, a app arranca em **H2 em memória** (perde os dados a cada reinício) —
> útil para um primeiro deploy de teste, não para produção.

## Build/run local

```bash
./mvnw test            # 64 testes
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # H2 + chaves de teste
```
