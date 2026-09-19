package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoOrigem;
import com.castilhodigital.facilitei.agenda.AgendamentoStatus;
import com.castilhodigital.facilitei.agenda.AgendamentoView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Agendamento como o dono ve no painel: com os dados do cliente, o estado do sinal e a origem. */
public record AgendamentoAdminResponse(
        Long id,
        UUID codigo,
        AgendamentoStatus status,
        AgendamentoOrigem origem,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        Long profissionalId,
        String profissionalNome,
        String clienteNome,
        String clienteTelefone,
        List<AgendamentoPublicoResponse.Servico> servicos,
        BigDecimal valorTotal,
        BigDecimal valorSinal,
        boolean sinalConfirmado,
        OffsetDateTime expiraEm
) {

    public static AgendamentoAdminResponse de(AgendamentoView agendamento) {
        ZoneId fuso = agendamento.fuso();
        return new AgendamentoAdminResponse(
                agendamento.id(),
                agendamento.codigoPublico(),
                agendamento.status(),
                agendamento.origem(),
                agendamento.inicio().atZone(fuso).toOffsetDateTime(),
                agendamento.fim().atZone(fuso).toOffsetDateTime(),
                agendamento.profissionalId(),
                agendamento.profissionalNome(),
                agendamento.clienteNome(),
                agendamento.clienteTelefone(),
                agendamento.servicos().stream()
                        .map(servico -> new AgendamentoPublicoResponse.Servico(servico.nome(), servico.duracaoMin(), servico.preco()))
                        .toList(),
                agendamento.valorTotal(),
                agendamento.valorSinal(),
                agendamento.sinalConfirmadoEm() != null,
                agendamento.expiraEm() == null ? null : agendamento.expiraEm().atZone(fuso).toOffsetDateTime());
    }

}
