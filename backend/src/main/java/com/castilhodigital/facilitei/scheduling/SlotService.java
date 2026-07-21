package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private final SlotRepository slotRepository;
    private final TenantService tenantService;

    /** Listagem publica (endpoint sem autenticacao da etapa 4) por slug + dia. */
    @Transactional(readOnly = true)
    public List<Slot> listarDisponiveisPorSlug(String tenantSlug, LocalDate data) {
        Tenant tenant = tenantService.buscarPorSlug(tenantSlug);
        OffsetDateTime inicio = data.atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fim = data.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();
        return slotRepository.findDisponiveisComServico(tenant.getId(), SlotStatus.DISPONIVEL, inicio, fim);
    }

    /** Agenda do admin (dia ou semana, todos os status) - dataInicio/dataFim sao inclusivos. */
    @Transactional(readOnly = true)
    public List<Slot> listarAgendaPorTenant(Long tenantId, LocalDate dataInicio, LocalDate dataFim) {
        OffsetDateTime inicio = dataInicio.atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fim = dataFim.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();
        return slotRepository.findAgendaComServico(tenantId, inicio, fim);
    }

    /** Marca o slot como RESERVADO ao iniciar um booking (cobranca Pix ainda pendente). */
    @Transactional
    public Slot reservar(Long slotId) {
        Slot slot = buscarPorId(slotId);
        if (slot.getStatus() != SlotStatus.DISPONIVEL) {
            throw new RegraDeNegocioException("Este horario nao esta mais disponivel.");
        }
        slot.setStatus(SlotStatus.RESERVADO);
        return slot;
    }

    /** Chamado pelo webhook do Asaas (etapa 6) quando o pagamento do sinal e confirmado. */
    @Transactional
    public void confirmar(Long slotId) {
        buscarPorId(slotId).setStatus(SlotStatus.CONFIRMADO);
    }

    /** Chamado quando uma cobranca Pix expira sem pagamento, devolvendo o horario para a agenda publica. */
    @Transactional
    public void liberar(Long slotId) {
        buscarPorId(slotId).setStatus(SlotStatus.DISPONIVEL);
    }

    @Transactional(readOnly = true)
    public Slot buscarPorId(Long slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Slot nao encontrado (id=" + slotId + ")."));
    }

    /**
     * Usado pelo endpoint publico de criar reserva: confirma que o slot
     * pertence ao tenant do slug informado antes de reservar, para que um
     * cliente nao consiga reservar o horario de outro negocio so adivinhando
     * o id do slot na URL/payload.
     */
    @Transactional(readOnly = true)
    public Slot buscarPorIdETenant(Long slotId, Long tenantId) {
        return slotRepository.findByIdAndTenantId(slotId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Slot nao encontrado (id=" + slotId + ")."));
    }

}
