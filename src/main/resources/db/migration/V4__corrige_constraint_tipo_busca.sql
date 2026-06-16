ALTER TABLE jobs_busca DROP CONSTRAINT IF EXISTS chk_jobs_tipo;

ALTER TABLE jobs_busca
    ADD CONSTRAINT chk_jobs_tipo CHECK (tipo IN ('NICHO', 'NOME'));
