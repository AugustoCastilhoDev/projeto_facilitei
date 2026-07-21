package com.castilhodigital.facilitei.payment;

import java.math.BigDecimal;

public record PixChargeRequest(
        String clienteNome,
        String clienteTelefone,
        String clienteCpfCnpj,
        BigDecimal valor,
        String referenciaExterna
) {
}
