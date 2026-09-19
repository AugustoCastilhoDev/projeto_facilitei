package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Retrato de um agendamento com tudo que as telas e as notificacoes precisam,
 * ja carregado. Os services devolvem isto (nao a entidade): com
 * open-in-view=false, ler profissional/servicos de uma entidade fora da
 * transacao estoura LazyInitializationException - o bug que a agenda antiga
 * teve varias vezes. Por isso a montagem (de) so pode ser chamada DENTRO da
 * transacao que carregou a entidade.
 */
public record AgendamentoView(
        Long id,
        UUID codigoPublico,
        Long tenantId,
        String tenantSlug,
        String tenantNome,
        ZoneId fuso,
        Long profissionalId,
        String profissionalNome,
        String clienteNome,
        String clienteTelefone,
        Instant inicio,
        Instant fim,
        AgendamentoStatus status,
        AgendamentoOrigem origem,
        BigDecimal valorTotal,
        BigDecimal valorSinal,
        Instant expiraEm,
        String cobrancaExternaId,
        String pixPayload,
        Instant sinalConfirmadoEm,
        List<Servico> servicos
) {

    public record Servico(String nome, int duracaoMin, BigDecimal preco) {
    }

    public static AgendamentoView de(Agendamento agendamento) {
        Tenant tenant = agendamento.getTenant();
        return new AgendamentoView(
                agendamento.getId(),
                agendamento.getCodigoPublico(),
                tenant.getId(),
                tenant.getSlug(),
                tenant.getNome(),
                ZoneId.of(tenant.getFusoHorario()),
                agendamento.getProfissional().getId(),
                agendamento.getProfissional().getNome(),
                agendamento.getClienteNome(),
                agendamento.getClienteTelefone(),
                agendamento.getInicio(),
                agendamento.getFim(),
                agendamento.getStatus(),
                agendamento.getOrigem(),
                agendamento.getValorTotal(),
                agendamento.getValorSinal(),
                agendamento.getExpiraEm(),
                agendamento.getAsaasPaymentId(),
                agendamento.getPixPayload(),
                agendamento.getSinalConfirmadoEm(),
                agendamento.getServicos().stream()
                        .map(servico -> new Servico(servico.getNome(), servico.getDuracaoMin(), servico.getPreco()))
                        .toList());
    }

}
