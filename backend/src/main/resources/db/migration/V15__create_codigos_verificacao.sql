CREATE TABLE codigos_verificacao (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    telefone    VARCHAR(20) NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    expira_em   TIMESTAMPTZ NOT NULL,
    usado_em    TIMESTAMPTZ,
    tentativas  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_codigos_verificacao_telefone ON codigos_verificacao(telefone);
