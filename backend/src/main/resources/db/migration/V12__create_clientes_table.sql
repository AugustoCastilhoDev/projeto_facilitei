CREATE TABLE clientes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    telefone    VARCHAR(20) NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    cpf_cnpj    VARCHAR(20),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_clientes_telefone UNIQUE (telefone)
);
