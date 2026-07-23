package com.castilhodigital.facilitei.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturaResponse(
        BigDecimal valor,
        LocalDate vencimento,
        String pixPayload,
        String pixQrCodeBase64
) {
}
