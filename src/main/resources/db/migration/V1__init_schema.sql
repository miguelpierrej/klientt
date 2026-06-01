-- Klientt — esquema inicial (SQL portável: H2 em modo MySQL e MySQL 8)
-- Baseado em docs/ARQUITETURA.md §5 (modelo de dados).
-- Nomes em português para alinhar com a documentação.

-- ---------------------------------------------------------------------------
-- Planos e utilizadores
-- ---------------------------------------------------------------------------
CREATE TABLE planos (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(80)     NOT NULL,
    limite_leads_mes    INT             NOT NULL,
    preco               DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE utilizadores (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    plano_id        BIGINT,
    criado_em       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_utilizadores_plano FOREIGN KEY (plano_id) REFERENCES planos (id)
);

-- ---------------------------------------------------------------------------
-- Empresas (cache partilhado entre buscas) + sinais enriquecidos
-- ---------------------------------------------------------------------------
CREATE TABLE empresas (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(255)    NOT NULL,
    cnpj            VARCHAR(14),
    telefone        VARCHAR(30),
    endereco        VARCHAR(255),
    cidade          VARCHAR(120),
    website         VARCHAR(255),
    lat             DOUBLE,
    lng             DOUBLE,
    fonte           VARCHAR(50),
    atualizado_em   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_empresas_cnpj   ON empresas (cnpj);
CREATE INDEX idx_empresas_cidade ON empresas (cidade);

CREATE TABLE sinais (
    id                      BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id              BIGINT          NOT NULL,
    nota_google             DECIMAL(2, 1),
    num_reviews             INT,
    site_existe             BOOLEAN,
    site_velocidade_ms      INT,
    site_https              BOOLEAN,
    site_num_paginas        INT,
    site_reputacao          VARCHAR(50),
    procon_evite_site       BOOLEAN         NOT NULL DEFAULT FALSE,
    -- (v2) reclameaqui_nota, reclameaqui_indice_resolucao, reclameaqui_url
    coletado_em             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sinais_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE
);

CREATE INDEX idx_sinais_empresa ON sinais (empresa_id);

CREATE TABLE empresa_redes (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id  BIGINT          NOT NULL,
    rede        VARCHAR(40)     NOT NULL,   -- instagram | facebook | linkedin | ...
    url         VARCHAR(255),
    seguidores  INT,
    CONSTRAINT fk_redes_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE
);

CREATE INDEX idx_empresa_redes_empresa ON empresa_redes (empresa_id);

-- ---------------------------------------------------------------------------
-- Jobs de busca (fluxo assíncrono) + resultados com score
-- ---------------------------------------------------------------------------
CREATE TABLE jobs_busca (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    utilizador_id   BIGINT,                  -- nullable até existir autenticação (Fase 4)
    tipo            VARCHAR(20)     NOT NULL,
    termo           VARCHAR(255)    NOT NULL,
    regiao          VARCHAR(120),
    estado          VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',
    criado_em       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    concluido_em    TIMESTAMP       NULL,
    CONSTRAINT fk_jobs_utilizador FOREIGN KEY (utilizador_id) REFERENCES utilizadores (id),
    CONSTRAINT chk_jobs_tipo   CHECK (tipo IN ('NICHO', 'NOME')),
    CONSTRAINT chk_jobs_estado CHECK (estado IN ('PENDENTE', 'A_PROCESSAR', 'CONCLUIDO', 'ERRO'))
);

CREATE INDEX idx_jobs_utilizador ON jobs_busca (utilizador_id);
CREATE INDEX idx_jobs_estado     ON jobs_busca (estado);

CREATE TABLE job_resultados (
    job_id      BIGINT      NOT NULL,
    empresa_id  BIGINT      NOT NULL,
    score       INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (job_id, empresa_id),
    CONSTRAINT fk_jobres_job     FOREIGN KEY (job_id) REFERENCES jobs_busca (id) ON DELETE CASCADE,
    CONSTRAINT fk_jobres_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
);

-- ---------------------------------------------------------------------------
-- Lista Procon-SP "Evite Sites" (pré-coletada, sync diário)
-- ---------------------------------------------------------------------------
CREATE TABLE procon_evite_sites (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dominio             VARCHAR(255)    NOT NULL,
    razao_social        VARCHAR(255),
    cnpj                VARCHAR(14),
    sincronizado_em     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_procon_dominio ON procon_evite_sites (dominio);
