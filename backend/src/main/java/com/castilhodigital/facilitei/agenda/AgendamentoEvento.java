package com.castilhodigital.facilitei.agenda;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Fato relevante do ciclo de vida de um agendamento, publicado DENTRO da
 * transacao que o produziu mas so entregue aos ouvintes DEPOIS do commit
 * (@TransactionalEventListener AFTER_COMMIT). Isso conserta um defeito da agenda
 * antiga: o WhatsApp saia no meio da transacao, entao um rollback deixava o
 * cliente com a mensagem de uma reserva que nao existia.
 *
 * Carrega os dados ja extraidos (nao a entidade) porque os ouvintes rodam
 * depois de a transacao fechar, sem sessao do Hibernate.
 */
public record AgendamentoEvento(
        Tipo tipo,
        Long agendamentoId,
        Long tenantId,
        String nomeNegocio,
        ZoneId fuso,
        String clienteNome,
        String clienteTelefone,
        Instant inicio,
        Instant expiraEm,
        String cobrancaExternaId,
        /** O sinal ja foi pago: cancelar a cobranca no gateway nao faz mais sentido (seria caso de devolucao). */
        boolean sinalConfirmado
) {

    public enum Tipo {
        /** Horario garantido: sem sinal, ou sinal confirmado. */
        CONFIRMADO,
        /** Horario segurado, cobranca criada, esperando o cliente pagar. */
        AGUARDANDO_PAGAMENTO,
        /** O prazo do sinal acabou e o horario voltou para a agenda. */
        EXPIRADO,
        /** O estabelecimento cancelou. */
        CANCELADO_PELO_NEGOCIO
    }

    public static AgendamentoEvento de(Tipo tipo, AgendamentoView agendamento) {
        return new AgendamentoEvento(tipo, agendamento.id(), agendamento.tenantId(), agendamento.tenantNome(),
                agendamento.fuso(), agendamento.clienteNome(), agendamento.clienteTelefone(), agendamento.inicio(),
                agendamento.expiraEm(), agendamento.cobrancaExternaId(), agendamento.sinalConfirmadoEm() != null);
    }

}
