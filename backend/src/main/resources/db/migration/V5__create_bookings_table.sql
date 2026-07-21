CREATE TABLE bookings (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slot_id             BIGINT NOT NULL REFERENCES slots(id) ON DELETE RESTRICT,
    cliente_nome        VARCHAR(150) NOT NULL,
    cliente_telefone    VARCHAR(20) NOT NULL,
    status_pagamento    VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    asaas_payment_id    VARCHAR(100),
    asaas_pix_payload   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookings_slot_id UNIQUE (slot_id),
    CONSTRAINT chk_bookings_status_pagamento CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'EXPIRADO', 'CANCELADO'))
);

CREATE INDEX idx_bookings_asaas_payment_id ON bookings(asaas_payment_id);
