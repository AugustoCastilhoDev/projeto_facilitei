package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinal;

/**
 * Resultado do checkout: o agendamento e, se houve sinal a pagar, a cobranca
 * criada (nula quando o agendamento ja nasceu confirmado, sem sinal).
 */
public record CheckoutAgendamento(AgendamentoView agendamento, CobrancaSinal cobranca) {
}
