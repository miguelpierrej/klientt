-- Perfil do cliente (ICP) para onboarding + relevância dos leads. 1:1 com o utilizador.
-- Listas (nichos/regiões/portes) guardadas como texto separado por vírgula (simples para o MVP).
CREATE TABLE perfil_cliente (
    utilizador_id     BIGINT       NOT NULL PRIMARY KEY,
    oferta            VARCHAR(255),                       -- o que o cliente vende/oferece
    nichos_alvo       VARCHAR(500),                       -- CNAEs-alvo, separados por vírgula
    regioes_alvo      VARCHAR(500),                       -- "Cidade/UF" ou UF, separados por vírgula
    portes_alvo       VARCHAR(120),                       -- MEI,MICRO,PEQUENA,GRANDE
    quer_sem_site     BOOLEAN      NOT NULL DEFAULT FALSE, -- prefere empresas sem site
    quer_simples_mei  BOOLEAN      NOT NULL DEFAULT FALSE, -- prefere optantes do Simples/MEI
    quer_com_contato  BOOLEAN      NOT NULL DEFAULT TRUE,  -- prioriza quem tem telefone/e-mail
    concluido         BOOLEAN      NOT NULL DEFAULT FALSE, -- onboarding preenchido ou pulado
    atualizado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_perfil_utilizador FOREIGN KEY (utilizador_id) REFERENCES utilizadores (id) ON DELETE CASCADE
);
