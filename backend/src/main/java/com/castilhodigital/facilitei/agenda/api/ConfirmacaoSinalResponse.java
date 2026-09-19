package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoStatus;
import com.castilhodigital.facilitei.agenda.AgendamentoView;
import com.castilhodigital.facilitei.pix.ReferenciaPix;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * O que o dono ve na tela do link do WhatsApp para conferir o Pix. So o dono tem o link (ver TokenConfirmacao),
 * por isso traz o nome do cliente; o telefone dele nao vai - o dono ja o tem no proprio WhatsApp e no painel.
 *
 * @param referencia   o identificador que o Pix leva no comprovante do banco, para achar o pagamento no extrato
 * @param podeConfirmar false depois que o horario foi cancelado
 * @param podeRecusar   so enquanto o dono ainda pode dizer "nao recebi" (o cliente avisou que pagou e ninguem decidiu)
 */
public record ConfirmacaoSinalResponse(
        AgendamentoStatus status,
        String negocioNome,
        String referencia,
        String clienteNome,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        String profissionalNome,
        List<AgendamentoPublicoResponse.Servico> servicos,
        BigDecimal valorSinal,
        OffsetDateTime pagamentoInformadoEm,
        OffsetDateTime expiraEm,
        boolean podeConfirmar,
        boolean podeRecusar
) {

    public static ConfirmacaoSinalResponse de(AgendamentoView agendamento) {
        ZoneId fuso = agendamento.fuso();
        AgendamentoStatus status = agendamento.status();
        boolean aindaPodeConfirmar = status.aguardaSinal() || status == AgendamentoStatus.EXPIRADO;
        return new ConfirmacaoSinalResponse(
                status,
                agendamento.tenantNome(),
                ReferenciaPix.doAgendamento(agendamento.id()),
                agendamento.clienteNome(),
                agendamento.inicio().atZone(fuso).toOffsetDateTime(),
                agendamento.fim().atZone(fuso).toOffsetDateTime(),
                agendamento.profissionalNome(),
                agendamento.servicos().stream()
                        .map(servico -> new AgendamentoPublicoResponse.Servico(servico.nome(), servico.duracaoMin(), servico.preco()))
                        .toList(),
                agendamento.valorSinal(),
                agendamento.pagamentoInformadoEm() == null ? null : agendamento.pagamentoInformadoEm().atZone(fuso).toOffsetDateTime(),
                agendamento.expiraEm() == null ? null : agendamento.expiraEm().atZone(fuso).toOffsetDateTime(),
                aindaPodeConfirmar,
                status == AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
    }

}
