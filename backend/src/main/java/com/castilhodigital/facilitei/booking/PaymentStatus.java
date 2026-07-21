package com.castilhodigital.facilitei.booking;

public enum PaymentStatus {
    /** Cobranca Pix criada no Asaas, aguardando pagamento. */
    PENDENTE,
    /** Webhook do Asaas confirmou o recebimento. */
    PAGO,
    /** Cobranca expirou sem pagamento (o slot volta a ficar DISPONIVEL). */
    EXPIRADO,
    /** Booking cancelado manualmente. */
    CANCELADO
}
