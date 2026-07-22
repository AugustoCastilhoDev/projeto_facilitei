ALTER TABLE bookings DROP CONSTRAINT chk_bookings_status_pagamento;

ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status_pagamento
    CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'SEM_SINAL', 'EXPIRADO', 'CANCELADO'));
