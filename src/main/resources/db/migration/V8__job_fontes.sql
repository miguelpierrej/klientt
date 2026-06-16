-- Conclusão de um job entre múltiplas fontes assíncronas (PLANO-DUAL-FONTE.md, Fase E).
-- O job só fica CONCLUIDO quando todas as fontes esperadas reportarem (scraper + CNPJ-por-CNAE).

ALTER TABLE jobs_busca ADD COLUMN fontes_esperadas  INT NOT NULL DEFAULT 1;
ALTER TABLE jobs_busca ADD COLUMN fontes_concluidas INT NOT NULL DEFAULT 0;
