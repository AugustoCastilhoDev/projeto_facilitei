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
    private static final List<SlotStatus> STATUS_OCUPADOS = List.of(SlotStatus.RESERVADO, SlotStatus.CONFIRMADO);

    private final SlotRepository slotRepository;
    private final TenantService tenantService;

    /**
     * Listagem publica (endpoint sem autenticacao da etapa 4) por slug + dia.
     * Exclui slots que, apesar de DISPONIVEL para o proprio servico, teriam
     * seu horario sobreposto por outro servico ja reservado/confirmado no
     * mesmo periodo (o negocio e modelado como um unico profissional/cadeira
     * - dois servicos diferentes nao podem acontecer ao mesmo tempo).
     */
    @Transactional(readOnly = true)
    public List<Slot> listarDisponiveisPorSlug(String tenantSlug, LocalDate data) {
        Tenant tenant = tenantService.buscarPorSlug(tenantSlug);
        OffsetDateTime inicio = data.atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fim = data.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();

        List<Slot> disponiveis = slotRepository.findDisponiveisComServico(tenant.getId(), SlotStatus.DISPONIVEL, inicio, fim);
        List<Slot> ocupados = slotRepository.findOcupadosNoIntervalo(tenant.getId(), STATUS_OCUPADOS, inicio, fim);

        return disponiveis.stream()
                .filter(candidato -> ocupados.stream().noneMatch(outro -> seSobrepoe(candidato, outro)))
                .toList();
    }

    /** Agenda do admin (dia ou semana, todos os status) - dataInicio/dataFim sao inclusivos. */
    @Transactional(readOnly = true)
    public List<Slot> listarAgendaPorTenant(Long tenantId, LocalDate dataInicio, LocalDate dataFim) {
        OffsetDateTime inicio = dataInicio.atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fim = dataFim.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();
        return slotRepository.findAgendaComServico(tenantId, inicio, fim);
    }

    /**
     * Marca o slot como RESERVADO ao iniciar um booking (cobranca Pix ainda
     * pendente). Rejeita se o horario conflitar com outro servico ja
     * reservado/confirmado no mesmo periodo - a listagem publica ja filtra
     * isso, mas a checagem e repetida aqui como defesa contra corrida entre
     * requisicoes concorrentes ou uma chamada direta a API.
     */
    @Transactional
    public Slot reservar(Long slotId) {
        Slot slot = buscarPorId(slotId);
        if (slot.getStatus() != SlotStatus.DISPONIVEL) {
            throw new RegraDeNegocioException("Este horario nao esta mais disponivel.");
        }

        OffsetDateTime inicioDoDia = slot.getDataHora().toLocalDate().atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fimDoDia = inicioDoDia.plusDays(1);
        List<Slot> ocupados = slotRepository.findOcupadosNoIntervalo(
                slot.getTenant().getId(), STATUS_OCUPADOS, inicioDoDia, fimDoDia);

        boolean conflito = ocupados.stream().anyMatch(outro -> seSobrepoe(slot, outro));
        if (conflito) {
            throw new RegraDeNegocioException("Este horario conflita com outro servico ja reservado no mesmo periodo.");
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

    /**
     * Dois servicos diferentes nao podem ocupar o mesmo profissional/cadeira
     * ao mesmo tempo, entao o conflito e por SOBREPOSICAO de intervalo (nao
     * so "mesmo horario exato"), ja que cada servico pode ter uma duracao
     * diferente (ex.: um corte de 30min pode se sobrepor parcialmente com
     * uma coloracao de 90min).
     */
    private boolean seSobrepoe(Slot a, Slot b) {
        if (a.getId() != null && a.getId().equals(b.getId())) {
            return false;
        }
        OffsetDateTime fimA = a.getDataHora().plusMinutes(a.getService().getDuracaoMin());
        OffsetDateTime fimB = b.getDataHora().plusMinutes(b.getService().getDuracaoMin());
        return a.getDataHora().isBefore(fimB) && fimA.isAfter(b.getDataHora());
    }

}
