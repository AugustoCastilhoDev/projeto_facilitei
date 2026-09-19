package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalResolver;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Quando um agendamento e liberado (prazo acabou ou o dono cancelou) sem o sinal
 * ter sido pago, cancela a cobranca no gateway. A cobranca da Asaas vence em
 * "amanha" (so tem granularidade de dia), muito depois do prazo de minutos do
 * agendamento - sem cancelar, o cliente ainda poderia pagar um horario que ja
 * voltou para a agenda. E melhor esforco: se falhar, o webhook tardio e o
 * alerta de AgendamentoPagamentoService cobrem o resto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AgendamentoCobrancaListener {

    private final TenantService tenantService;
    private final CobrancaSinalResolver cobrancaSinalResolver;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void cancelarCobrancaPendente(AgendamentoEvento evento) {
        boolean horarioLiberado = evento.tipo() == AgendamentoEvento.Tipo.EXPIRADO
                || evento.tipo() == AgendamentoEvento.Tipo.CANCELADO_PELO_NEGOCIO;
        if (!horarioLiberado || evento.cobrancaExternaId() == null || evento.sinalConfirmado()) {
            return;
        }
        try {
            Tenant tenant = tenantService.buscarPorId(evento.tenantId());
            cobrancaSinalResolver.exigir(tenant).cancelar(tenant, evento.cobrancaExternaId());
        } catch (RuntimeException ex) {
            log.warn("Nao foi possivel cancelar a cobranca {} do agendamento {}: {}",
                    evento.cobrancaExternaId(), evento.agendamentoId(), ex.getMessage());
        }
    }

}
