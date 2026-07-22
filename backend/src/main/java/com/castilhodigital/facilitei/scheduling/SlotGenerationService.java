package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
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
 * de funcionamento do PROFISSIONAL escolhido e da duracao do servico.
 * Idempotente: rodar de novo para o mesmo dia/profissional nao duplica
 * slots ja existentes.
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
    private final ProfissionalService profissionalService;
    private final SlotRepository slotRepository;

    @Transactional
    public List<Slot> gerarSlotsParaData(Long tenantId, Long profissionalId, Long serviceId, LocalDate data) {
        ServiceOffering service = serviceOfferingService.buscarPorIdETenant(tenantId, serviceId);
        if (!service.isAtivo()) {
            throw new RegraDeNegocioException("Nao e possivel gerar horarios para um servico desativado.");
        }

        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        if (!profissional.isAtivo()) {
            throw new RegraDeNegocioException("Nao e possivel gerar horarios para um profissional desativado.");
        }
        if (!profissional.getServicos().contains(service)) {
            throw new RegraDeNegocioException("Este profissional nao realiza este servico.");
        }

        LocalDateTime inicio = LocalDateTime.of(data, profissional.getHorarioAbertura());
        LocalDateTime fim = LocalDateTime.of(data, profissional.getHorarioFechamento());
        Duration duracao = Duration.ofMinutes(service.getDuracaoMin());

        List<Slot> novosSlots = new ArrayList<>();
        for (LocalDateTime candidato = inicio; !candidato.plus(duracao).isAfter(fim); candidato = candidato.plus(duracao)) {
            OffsetDateTime dataHora = candidato.atZone(ZONE_ID).toOffsetDateTime();
            if (slotRepository.existsByProfissionalIdAndServiceIdAndDataHora(profissionalId, serviceId, dataHora)) {
                continue;
            }

            Slot slot = new Slot();
            slot.setTenant(service.getTenant());
            slot.setService(service);
            slot.setProfissional(profissional);
            slot.setDataHora(dataHora);
            slot.setStatus(SlotStatus.DISPONIVEL);
            novosSlots.add(slot);
        }

        return slotRepository.saveAll(novosSlots);
    }

}
