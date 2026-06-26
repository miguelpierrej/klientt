-- Pivô só-API (PLANO-SO-API.md, Fase A): remover o scraper e o score de "dor digital".
-- A descoberta passa a ser 100% Casa dos Dados; o produto é uma lista contactável.

-- Sinais (nota/site) e redes vinham só do scraper Maps — já não existem.
DROP TABLE IF EXISTS sinais;
DROP TABLE IF EXISTS empresa_redes;

-- Score deixa de existir (sem sinais digitais para pontuar).
ALTER TABLE job_resultados DROP COLUMN score;

-- Colunas do enriquecimento Maps por empresa.
ALTER TABLE empresas DROP COLUMN confirmado_maps;
ALTER TABLE empresas DROP COLUMN endereco_maps;
ALTER TABLE empresas DROP COLUMN endereco_divergente;

-- Conclusão do job: já não há fontes paralelas nem enriquecimentos a contar.
ALTER TABLE jobs_busca DROP COLUMN fontes_esperadas;
ALTER TABLE jobs_busca DROP COLUMN fontes_concluidas;
ALTER TABLE jobs_busca DROP COLUMN descoberta_concluida;
ALTER TABLE jobs_busca DROP COLUMN enriquecimentos_esperados;
ALTER TABLE jobs_busca DROP COLUMN enriquecimentos_recebidos;
