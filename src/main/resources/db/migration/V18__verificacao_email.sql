-- Verificação de email em dois passos (anti-fraude): o utilizador confirma o email por um link
-- antes de poder entrar. Token de uso único com validade.
ALTER TABLE utilizadores ADD COLUMN email_verificado            BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE utilizadores ADD COLUMN token_verificacao           VARCHAR(100);
ALTER TABLE utilizadores ADD COLUMN token_verificacao_expira_em TIMESTAMP    NULL;

CREATE INDEX idx_utilizadores_token_verificacao ON utilizadores (token_verificacao);

-- Contas já existentes (anteriores à feature) ficam verificadas, para não as trancar fora.
UPDATE utilizadores SET email_verificado = TRUE;
