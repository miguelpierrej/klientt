-- Dados cadastrais do CNPJ (Receita/BrasilAPI) para exibição no detalhe do lead.

ALTER TABLE empresas ADD COLUMN razao_social        VARCHAR(255);
ALTER TABLE empresas ADD COLUMN nome_fantasia       VARCHAR(255);
ALTER TABLE empresas ADD COLUMN situacao_cadastral  VARCHAR(60);
ALTER TABLE empresas ADD COLUMN data_abertura       DATE;
ALTER TABLE empresas ADD COLUMN capital_social      DECIMAL(15, 2);
ALTER TABLE empresas ADD COLUMN porte               VARCHAR(60);
ALTER TABLE empresas ADD COLUMN natureza_juridica   VARCHAR(255);
ALTER TABLE empresas ADD COLUMN cnae_principal      VARCHAR(255);
ALTER TABLE empresas ADD COLUMN optante_simples     BOOLEAN;
ALTER TABLE empresas ADD COLUMN optante_mei         BOOLEAN;
