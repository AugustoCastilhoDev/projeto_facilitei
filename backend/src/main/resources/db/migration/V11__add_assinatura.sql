ALTER TABLE tenants ADD COLUMN cpf_cnpj VARCHAR(20);
ALTER TABLE tenants ADD COLUMN plano VARCHAR(20) NOT NULL DEFAULT 'BASICO';
ALTER TABLE tenants ADD COLUMN assinatura_status VARCHAR(20) NOT NULL DEFAULT 'ATIVA';
ALTER TABLE tenants ADD COLUMN trial_ate DATE;
ALTER TABLE tenants ADD COLUMN proxima_cobranca_em DATE;

CREATE TABLE faturas (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plano VARCHAR(20) NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    asaas_payment_id VARCHAR(60),
    status VARCHAR(20) NOT NULL,
    competencia DATE NOT NULL,
    vencimento DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_faturas_asaas_payment_id ON faturas(asaas_payment_id);
