package com.castilhodigital.facilitei.agenda.sinal;

import java.math.BigDecimal;

/**
 * O que um meio de pagamento precisa para cobrar o sinal. agendamentoId e
 * nulo na validacao previa (o agendamento ainda nao foi gravado).
 */
public record SolicitacaoCobranca(
        Long agendamentoId,
        String clienteNome,
        String clienteTelefone,
        String clienteCpfCnpj,
        BigDecimal valor
) {
}
