-- No-op (mantido para preservar o histórico Flyway).
--
-- Originalmente fazia DROP CONSTRAINT IF EXISTS chk_jobs_tipo + ADD, sintaxe que
-- funciona no H2 mas NÃO no MySQL 8 (não suporta DROP CONSTRAINT IF EXISTS).
-- Como o V1 já cria a constraint correta CHECK (tipo IN ('NICHO','NOME')), esta
-- migração é redundante em instalações novas. Esvaziada para o schema migrar tanto
-- em H2 (testes) como em MySQL (dev/produção).
SELECT 1;
