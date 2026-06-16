-- Dados de subscrição Stripe no utilizador.
ALTER TABLE utilizadores ADD COLUMN stripe_customer_id     VARCHAR(64);
ALTER TABLE utilizadores ADD COLUMN stripe_subscription_id VARCHAR(64);
ALTER TABLE utilizadores ADD COLUMN subscription_status    VARCHAR(40);
