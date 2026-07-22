CREATE TABLE profissionais (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    nome               VARCHAR(150) NOT NULL,
    horario_abertura   TIME NOT NULL,
    horario_fechamento TIME NOT NULL,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_profissionais_tenant_id ON profissionais(tenant_id);

CREATE TABLE profissional_servicos (
    profissional_id BIGINT NOT NULL REFERENCES profissionais(id) ON DELETE CASCADE,
    service_id      BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (profissional_id, service_id)
);

-- Backfill: 1 profissional por tenant existente, com o expediente que estava em
-- tenants, vinculado a TODOS os servicos ja cadastrados daquele tenant (preserva
-- o funcionamento dos dados existentes apos a migracao).
INSERT INTO profissionais (tenant_id, nome, horario_abertura, horario_fechamento, ativo, created_at, updated_at)
SELECT id, 'Profissional 1', horario_abertura, horario_fechamento, true, now(), now() FROM tenants;

INSERT INTO profissional_servicos (profissional_id, service_id)
SELECT p.id, s.id FROM profissionais p JOIN services s ON s.tenant_id = p.tenant_id;

ALTER TABLE slots ADD COLUMN profissional_id BIGINT;

UPDATE slots s SET profissional_id = (SELECT p.id FROM profissionais p WHERE p.tenant_id = s.tenant_id LIMIT 1);

ALTER TABLE slots ALTER COLUMN profissional_id SET NOT NULL;
ALTER TABLE slots ADD CONSTRAINT fk_slots_profissional FOREIGN KEY (profissional_id) REFERENCES profissionais(id);

ALTER TABLE slots DROP CONSTRAINT uq_slots_service_datahora;
ALTER TABLE slots ADD CONSTRAINT uq_slots_profissional_service_datahora UNIQUE (profissional_id, service_id, data_hora);

CREATE INDEX idx_slots_profissional_id ON slots(profissional_id);

ALTER TABLE tenants DROP COLUMN horario_abertura;
ALTER TABLE tenants DROP COLUMN horario_fechamento;
