-- Catálogo CNAE (subclasses) do IBGE — armazenado para validar/descrever códigos e permitir
-- busca por descrição na resolução nicho→CNAE (PLANO-DUAL-FONTE.md). Semeado a partir do
-- recurso cnae/subclasses.csv pelo CnaeCatalogoSeeder.

CREATE TABLE cnae (
    codigo      VARCHAR(7)   NOT NULL PRIMARY KEY,
    descricao   VARCHAR(255) NOT NULL
);
