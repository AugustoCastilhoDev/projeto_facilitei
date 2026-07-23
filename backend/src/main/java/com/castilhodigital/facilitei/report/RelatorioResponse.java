package com.castilhodigital.facilitei.report;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioResponse(
        BigDecimal faturamentoTotal,
        int totalReservasConfirmadas,
        int totalComparecimentosMarcados,
        int totalNaoComparecimentos,
        BigDecimal taxaNaoComparecimentoPercentual,
        List<ClienteRecorrenteResponse> clientesRecorrentes
) {
}
