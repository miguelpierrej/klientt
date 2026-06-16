-- Contatos como cidadão de primeira classe (PLANO-DUAL-FONTE.md, Fase C).
-- Vários meios de contacto por empresa, com origem (fonte) e confiança (verificado).
-- Os campos empresas.telefone/empresas.email ficam como derivados do contato principal.

CREATE TABLE contatos (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT          NOT NULL,
    tipo        VARCHAR(20)     NOT NULL,   -- telefone | whatsapp | email | instagram | ...
    valor       VARCHAR(255)    NOT NULL,
    fonte       VARCHAR(50),                -- origem (ex.: google_maps, receita)
    principal   BOOLEAN         NOT NULL DEFAULT FALSE,
    verificado  BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contatos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_contatos_unico   ON contatos (empresa_id, tipo, valor);
CREATE INDEX        idx_contatos_empresa ON contatos (empresa_id);
