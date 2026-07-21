package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera os slots agendaveis de um servico para um dia, a partir do horario
 * de funcionamento do tenant e da duracao do servico. Idempotente: rodar
 * de novo para o mesmo dia nao duplica slots ja existentes.
 */
@Service
@RequiredArgsConstructor
public class SlotGenerationService {

    /**
     * Fuso fixo para o MVP (publico-alvo inicial: negocios no Brasil).
     * Suportar tenants em fusos diferentes exigiria uma coluna de timezone
     * em "tenants" - deixado fora do escopo do MVP.
     */
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private final ServiceOfferingService serviceOfferingService;
    private final SlotRepository slotRepository;

    @Transactional
    public List<Slot> gerarSlotsParaData(Long tenantId, Long serviceId, LocalDate data) {
        ServiceOffering service = serviceOfferingService.buscarPorIdETenant(tenantId, serviceId);
        Tenant tenant = service.getTenant();

        LocalDateTime inicio = LocalDateTime.of(data, tenant.getHorarioAbertura());
        LocalDateTime fim = LocalDateTime.of(data, tenant.getHorarioFechamento());
        Duration duracao = Duration.ofMinutes(service.getDuracaoMin());

        List<Slot> novosSlots = new ArrayList<>();
        for (LocalDateTime candidato = inicio; !candidato.plus(duracao).isAfter(fim); candidato = candidato.plus(duracao)) {
            OffsetDateTime dataHora = candidato.atZone(ZONE_ID).toOffsetDateTime();
            if (slotRepository.existsByServiceIdAndDataHora(serviceId, dataHora)) {
                continue;
            }

            Slot slot = new Slot();
            slot.setTenant(tenant);
            slot.setService(service);
            slot.setDataHora(dataHora);
            slot.setStatus(SlotStatus.DISPONIVEL);
            novosSlots.add(slot);
        }

        return slotRepository.saveAll(novosSlots);
    }

}
