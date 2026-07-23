package com.castilhodigital.facilitei.report;

import java.time.OffsetDateTime;

public record ClienteRecorrenteResponse(
        String clienteNome,
        String clienteTelefone,
        int totalAgendamentos,
        OffsetDateTime ultimoAgendamento
) {
}
