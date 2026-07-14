-- Créditos pré-pagos (Stripe): saldo de leads comprados no utilizador + histórico de compras.
ALTER TABLE utilizadores ADD COLUMN creditos_leads INT NOT NULL DEFAULT 0;

CREATE TABLE pagamentos (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    utilizador_id     BIGINT       NOT NULL,
    stripe_session_id VARCHAR(255) NOT NULL,   -- idempotência: credita uma só vez
    leads             INT          NOT NULL,
    valor_centavos    INT,
    moeda             VARCHAR(10),
    estado            VARCHAR(30),
    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagamentos_utilizador FOREIGN KEY (utilizador_id) REFERENCES utilizadores (id),
    CONSTRAINT uq_pagamentos_session    UNIQUE (stripe_session_id)
);

CREATE INDEX idx_pagamentos_utilizador ON pagamentos (utilizador_id);
