-- Recria empresa_redes (removida no V12 com o pivô só-API) e cria empresa_socios.
-- Redes sociais e sócios (QSA) passam a ser recolhidos na descoberta/enriquecimento.

CREATE TABLE empresa_redes (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    rede        VARCHAR(30)  NOT NULL,   -- instagram | facebook | linkedin | youtube | tiktok | x
    url         VARCHAR(255) NOT NULL,
    CONSTRAINT fk_redes_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE,
    CONSTRAINT uq_empresa_rede  UNIQUE (empresa_id, rede)
);

CREATE INDEX idx_empresa_redes_empresa ON empresa_redes (empresa_id);

CREATE TABLE empresa_socios (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id   BIGINT       NOT NULL,
    nome         VARCHAR(255) NOT NULL,
    qualificacao VARCHAR(120),
    faixa_etaria VARCHAR(60),
    desde        DATE,
    CONSTRAINT fk_socios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE
);

CREATE INDEX idx_empresa_socios_empresa ON empresa_socios (empresa_id);
