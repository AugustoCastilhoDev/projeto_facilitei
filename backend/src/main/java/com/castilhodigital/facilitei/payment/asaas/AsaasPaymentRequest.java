package com.castilhodigital.facilitei.payment.asaas;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AsaasPaymentRequest(
        String customer,
        String billingType,
        BigDecimal value,
        LocalDate dueDate,
        String externalReference
) {
}
