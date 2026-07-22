package com.castilhodigital.facilitei.booking;

public enum PaymentStatus {
    /** Cobranca Pix criada no Asaas, aguardando pagamento. */
    PENDENTE,
    /** Webhook do Asaas confirmou o recebimento. */
    PAGO,
    /** Servico sem sinal (sinalPercentual = 0): reserva confirmada direto, sem cobranca Pix - pagamento e feito no local. */
    SEM_SINAL,
    /** Cobranca expirou sem pagamento (o slot volta a ficar DISPONIVEL). */
    EXPIRADO,
    /** Booking cancelado manualmente. */
    CANCELADO
}
