package com.castilhodigital.facilitei.scheduling;

public enum SlotStatus {
    /** Livre para reserva. */
    DISPONIVEL,
    /** Cobranca Pix gerada, aguardando confirmacao de pagamento. */
    RESERVADO,
    /** Pagamento do sinal confirmado via webhook do Asaas. */
    CONFIRMADO
}
