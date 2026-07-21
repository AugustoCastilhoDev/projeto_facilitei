CREATE TABLE slots (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    service_id  BIGINT NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    data_hora   TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_slots_status CHECK (status IN ('DISPONIVEL', 'RESERVADO', 'CONFIRMADO')),
    CONSTRAINT uq_slots_service_datahora UNIQUE (service_id, data_hora)
);

CREATE INDEX idx_slots_tenant_id ON slots(tenant_id);
CREATE INDEX idx_slots_status ON slots(status);
