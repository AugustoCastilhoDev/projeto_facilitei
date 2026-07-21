CREATE TABLE services (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    nome              VARCHAR(150) NOT NULL,
    duracao_min       INTEGER NOT NULL CHECK (duracao_min > 0),
    preco             NUMERIC(10,2) NOT NULL CHECK (preco >= 0),
    sinal_percentual  NUMERIC(5,2) NOT NULL CHECK (sinal_percentual >= 0 AND sinal_percentual <= 100),
    ativo             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_services_tenant_id ON services(tenant_id);
