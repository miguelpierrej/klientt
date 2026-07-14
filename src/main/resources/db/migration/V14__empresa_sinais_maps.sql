-- Sinais Google Maps por empresa (recolhidos no enriquecimento): nota e nº de avaliações.
ALTER TABLE empresas ADD COLUMN nota       DOUBLE;
ALTER TABLE empresas ADD COLUMN avaliacoes INT;
