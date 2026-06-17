-- Sinal "confirmado no Maps" do enriquecimento (PLANO-DUAL-FONTE.md, Fase 2 / melhoria de match).
-- null = ainda não enriquecido; false = enriquecido mas não encontrado no Maps; true = encontrado.
ALTER TABLE empresas ADD COLUMN confirmado_maps BOOLEAN;
