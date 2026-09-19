package com.castilhodigital.facilitei.agenda;

import java.util.Arrays;
import java.util.List;

/**
 * Ciclo de vida de um agendamento. O que separa "ocupa o horario" de "nao
 * ocupa" e o que a constraint de exclusao do banco usa para impedir double
 * booking (ver V16) - por isso AgendamentoStatusTest confere que esta lista e
 * a do banco continuam iguais.
 *
 * Fluxo tipico com sinal:  AGUARDANDO_PAGAMENTO -> CONFIRMADO -> CONCLUIDO | FALTOU.
 * Fluxo tipico com Pix proprio (confirmacao manual do dono):
 *   AGUARDANDO_PAGAMENTO -> AGUARDANDO_CONFIRMACAO -> CONFIRMADO -> ...
 * Sem sinal, nasce CONFIRMADO. Saidas sem atendimento: EXPIRADO, CANCELADO_*.
 */
public enum AgendamentoStatus {

    /** Horario segurado por um prazo (expira_em) esperando o cliente pagar o sinal. */
    AGUARDANDO_PAGAMENTO(true),

    /** Cliente disse que pagou (Pix proprio); horario segurado ate o dono confirmar ou o prazo acabar. */
    AGUARDANDO_CONFIRMACAO(true),

    CONFIRMADO(true),

    /** Cliente foi atendido. */
    CONCLUIDO(true),

    /** Cliente nao compareceu - base da taxa de nao comparecimento dos relatorios. */
    FALTOU(true),

    /** O prazo acabou sem pagamento/confirmacao; o horario voltou para a agenda. */
    EXPIRADO(false),

    CANCELADO_CLIENTE(false),

    CANCELADO_NEGOCIO(false);

    /** Status que ocupam o horario do profissional - o mesmo predicado da constraint ex_agendamento_sem_sobreposicao. */
    public static final List<AgendamentoStatus> QUE_OCUPAM_HORARIO =
            Arrays.stream(values()).filter(AgendamentoStatus::ocupaHorario).toList();

    private final boolean ocupaHorario;

    AgendamentoStatus(boolean ocupaHorario) {
        this.ocupaHorario = ocupaHorario;
    }

    public boolean ocupaHorario() {
        return ocupaHorario;
    }

    /** Segura o horario por tempo limitado: exige expira_em (constraint chk_agendamentos_espera_tem_prazo). */
    public boolean aguardaSinal() {
        return this == AGUARDANDO_PAGAMENTO || this == AGUARDANDO_CONFIRMACAO;
    }

}
