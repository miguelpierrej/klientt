-- Recuperação de senha: token de uso único com validade (curta), separado do de verificação.
ALTER TABLE utilizadores ADD COLUMN token_reset           VARCHAR(100);
ALTER TABLE utilizadores ADD COLUMN token_reset_expira_em TIMESTAMP NULL;

CREATE INDEX idx_utilizadores_token_reset ON utilizadores (token_reset);
