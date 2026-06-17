-- Enriquecimento Maps sobre os leads da Casa dos Dados (PLANO-DUAL-FONTE.md, Fase 2).
-- O scraper procura no Maps por nome fantasia, confirma o endereço e traz sinais (nota/site/redes).

-- Endereço do Maps + flag de divergência face ao endereço cadastral (Receita/Casa dos Dados).
ALTER TABLE empresas ADD COLUMN endereco_maps        VARCHAR(255);
ALTER TABLE empresas ADD COLUMN endereco_divergente  BOOLEAN;

-- Conclusão do job: descoberta (Casa dos Dados) + N enriquecimentos (1 por empresa).
ALTER TABLE jobs_busca ADD COLUMN descoberta_concluida       BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE jobs_busca ADD COLUMN enriquecimentos_esperados  INT     NOT NULL DEFAULT 0;
ALTER TABLE jobs_busca ADD COLUMN enriquecimentos_recebidos  INT     NOT NULL DEFAULT 0;
