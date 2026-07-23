package com.castilhodigital.facilitei.billing;

import java.time.LocalDate;

public record AssinaturaResponse(
        Plano plano,
        AssinaturaStatus status,
        LocalDate trialAte,
        LocalDate proximaCobrancaEm,
        FaturaResponse faturaPendente
) {
}
