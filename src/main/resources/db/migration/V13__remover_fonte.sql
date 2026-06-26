-- Pivô só-API (PLANO-SO-API.md, Fase C): uma só fonte de descoberta (Casa dos Dados),
-- por isso o marcador de origem deixa de fazer sentido.
ALTER TABLE empresas DROP COLUMN fonte;
ALTER TABLE contatos DROP COLUMN fonte;
