CREATE TABLE tenants (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    slug                VARCHAR(80)  NOT NULL,
    asaas_wallet_id     VARCHAR(100),
    horario_abertura    TIME NOT NULL,
    horario_fechamento  TIME NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenants_slug UNIQUE (slug)
);
