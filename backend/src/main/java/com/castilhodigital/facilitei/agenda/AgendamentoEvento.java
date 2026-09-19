package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

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
        /** WhatsApp do dono (55 + DDD + numero); nulo se ainda nao cadastrou. */
        String telefoneDono,
        ZoneId fuso,
        String clienteNome,
        String clienteTelefone,
        Instant inicio,
        Instant expiraEm,
        BigDecimal valorSinal,
        List<String> servicos,
        MeioCobranca meioCobranca,
        String cobrancaExternaId,
        /** O sinal ja foi pago: cancelar a cobranca no gateway nao faz mais sentido (seria caso de devolucao). */
        boolean sinalConfirmado,
        /** O cliente ja tinha avisado que pagou (Pix proprio): muda o texto de quando o prazo acaba. */
        boolean pagamentoInformado,
        /** Segredo do link do dono; so existe em PAGAMENTO_INFORMADO, so em memoria (o banco guarda o hash). */
        String tokenConfirmacao
) {

    public enum Tipo {
        /** Horario garantido: sem sinal, ou sinal confirmado. */
        CONFIRMADO(false),
        /** Horario segurado, cobranca criada, esperando o cliente pagar. */
        AGUARDANDO_PAGAMENTO(false),
        /** O prazo do sinal acabou e o horario voltou para a agenda. */
        EXPIRADO(false),
        /** O estabelecimento cancelou. */
        CANCELADO_PELO_NEGOCIO(false),
        /** O dono conferiu o extrato, nao achou o Pix e recusou: o horario voltou para a agenda. */
        PAGAMENTO_NAO_LOCALIZADO(false),
        /** O cliente avisou que pagou (Pix proprio): o dono precisa conferir e confirmar. */
        PAGAMENTO_INFORMADO(true);

        private final boolean paraODono;

        Tipo(boolean paraODono) {
            this.paraODono = paraODono;
        }

        /** A mensagem deste evento vai para o WhatsApp do dono, nao para o do cliente. */
        public boolean paraODono() {
            return paraODono;
        }
    }

    public static AgendamentoEvento de(Tipo tipo, AgendamentoView agendamento) {
        return de(tipo, agendamento, null);
    }

    public static AgendamentoEvento de(Tipo tipo, AgendamentoView agendamento, String tokenConfirmacao) {
        return new AgendamentoEvento(tipo, agendamento.id(), agendamento.tenantId(), agendamento.tenantNome(),
                agendamento.telefoneDono(), agendamento.fuso(), agendamento.clienteNome(), agendamento.clienteTelefone(),
                agendamento.inicio(), agendamento.expiraEm(), agendamento.valorSinal(),
                agendamento.servicos().stream().map(AgendamentoView.Servico::nome).toList(),
                agendamento.meioCobranca(), agendamento.cobrancaExternaId(), agendamento.sinalConfirmadoEm() != null,
                agendamento.pagamentoInformadoEm() != null, tokenConfirmacao);
    }

    /** Sem o token: o toString sai em log/erro e o token e um segredo que so pode ir para o WhatsApp do dono. */
    @Override
    public String toString() {
        return "AgendamentoEvento[tipo=" + tipo + ", agendamentoId=" + agendamentoId + ", tenantId=" + tenantId + "]";
    }

}
