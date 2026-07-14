-- "Carregar mais": guardar o CNAE confirmado e o cursor da fonte (Minha Receita)
-- para continuar a mesma busca na página seguinte.
-- NB: a coluna chama-se cursor_fonte (cursor é palavra reservada no MySQL).
ALTER TABLE jobs_busca ADD COLUMN cnae         VARCHAR(20);
ALTER TABLE jobs_busca ADD COLUMN cursor_fonte VARCHAR(100);
