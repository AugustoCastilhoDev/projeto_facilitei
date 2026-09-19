package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.DisponibilidadeCalculator.IntervaloDiario;
import com.castilhodigital.facilitei.agenda.DisponibilidadeCalculator.PeriodoOcupado;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega do banco o que a DisponibilidadeCalculator precisa (expediente,
 * bloqueios, agendamentos ativos, parametros do negocio) e monta os horarios
 * livres de um dia. A listagem e so um convite: quem garante que dois clientes
 * nao levam o mesmo horario e a constraint de exclusao do banco no momento de
 * criar o agendamento, nao esta consulta.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    /** Um horario oferecido ao cliente: quando comeca, quando termina e com qual profissional. */
    public record HorarioDisponivel(Instant inicio, Instant fim, Long profissionalId, String profissionalNome) {
    }

    private final TenantService tenantService;
    private final ServiceOfferingService serviceOfferingService;
    private final ProfissionalService profissionalService;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;

    /**
     * @param profissionalId profissional escolhido, ou nulo para "qualquer profissional": nesse caso
     *                       cada horario aparece uma vez, atribuido ao primeiro profissional (por nome) que esta livre
     * @param agora          recebido de fora (nao le o relogio) para o calculo ser testavel
     */
    @Transactional(readOnly = true)
    public List<HorarioDisponivel> listar(Long tenantId, Collection<Long> servicoIds, Long profissionalId,
                                           LocalDate data, Instant agora) {
        Tenant tenant = tenantService.buscarPorId(tenantId);
        List<ServiceOffering> servicos = buscarServicosAtivos(tenantId, servicoIds);
        List<Profissional> candidatos = buscarProfissionaisCandidatos(tenantId, profissionalId, servicos);
        if (candidatos.isEmpty()) {
            return List.of();
        }

        ZoneId fuso = ZoneId.of(tenant.getFusoHorario());
        Instant inicioDoDia = data.atStartOfDay(fuso).toInstant();
        Instant fimDoDia = data.plusDays(1).atStartOfDay(fuso).toInstant();
        Duration duracao = Duration.ofMinutes(servicos.stream().mapToInt(ServiceOffering::getDuracaoMin).sum());

        Instant limiteInferior = agora.plus(Duration.ofMinutes(tenant.getAntecedenciaMinimaMin()));
        // "hoje" (no fuso do negocio) + horizonte dias, ate o fim desse dia
        Instant limiteSuperior = agora.atZone(fuso).toLocalDate()
                .plusDays(tenant.getHorizonteDias() + 1L).atStartOfDay(fuso).toInstant();

        List<Long> ids = candidatos.stream().map(BaseEntity::getId).toList();
        Map<Long, List<IntervaloDiario>> expedientePorProfissional = horarioTrabalhoRepository
                .findByProfissionalIdInAndDiaSemanaOrderByInicio(ids, data.getDayOfWeek()).stream()
                .collect(Collectors.groupingBy(h -> h.getProfissional().getId(),
                        Collectors.mapping(h -> new IntervaloDiario(h.getInicio(), h.getFim()), Collectors.toList())));
        Map<Long, List<PeriodoOcupado>> ocupadosPorProfissional = ocupadosPorProfissional(ids, inicioDoDia, fimDoDia);

        // TreeMap: ordena por inicio e, com putIfAbsent, o primeiro profissional livre "ganha" o horario
        TreeMap<Instant, HorarioDisponivel> resultado = new TreeMap<>();
        for (Profissional profissional : candidatos) {
            List<Instant> inicios = DisponibilidadeCalculator.horariosLivres(
                    data, fuso,
                    expedientePorProfissional.getOrDefault(profissional.getId(), List.of()),
                    ocupadosPorProfissional.getOrDefault(profissional.getId(), List.of()),
                    duracao, tenant.getIntervaloAgendaMin(), limiteInferior, limiteSuperior);
            for (Instant inicio : inicios) {
                resultado.putIfAbsent(inicio, new HorarioDisponivel(inicio, inicio.plus(duracao), profissional.getId(), profissional.getNome()));
            }
        }
        return new ArrayList<>(resultado.values());
    }

    private Map<Long, List<PeriodoOcupado>> ocupadosPorProfissional(List<Long> ids, Instant inicio, Instant fim) {
        Map<Long, List<PeriodoOcupado>> mapa = new HashMap<>();
        bloqueioRepository.findSobrepostos(ids, inicio, fim).forEach(bloqueio -> mapa
                .computeIfAbsent(bloqueio.getProfissional().getId(), id -> new ArrayList<>())
                .add(new PeriodoOcupado(bloqueio.getInicio(), bloqueio.getFim())));
        agendamentoRepository.findOcupados(ids, AgendamentoStatus.QUE_OCUPAM_HORARIO, inicio, fim).forEach(agendamento -> mapa
                .computeIfAbsent(agendamento.getProfissional().getId(), id -> new ArrayList<>())
                .add(new PeriodoOcupado(agendamento.getInicio(), agendamento.getFim())));
        return mapa;
    }

    List<ServiceOffering> buscarServicosAtivos(Long tenantId, Collection<Long> servicoIds) {
        if (servicoIds == null || servicoIds.isEmpty()) {
            throw new RegraDeNegocioException("Escolha pelo menos um servico.");
        }
        List<ServiceOffering> servicos = new ArrayList<>();
        for (Long servicoId : new LinkedHashSet<>(servicoIds)) {
            ServiceOffering servico = serviceOfferingService.buscarPorIdETenant(tenantId, servicoId);
            if (!servico.isAtivo()) {
                throw new RegraDeNegocioException("O servico '" + servico.getNome() + "' nao esta disponivel para agendamento.");
            }
            servicos.add(servico);
        }
        return servicos;
    }

    /** Profissional escolhido (validado) ou todos os ativos que realizam TODOS os servicos pedidos, na ordem de nome. */
    private List<Profissional> buscarProfissionaisCandidatos(Long tenantId, Long profissionalId, List<ServiceOffering> servicos) {
        Set<Long> idsDosServicos = servicos.stream().map(BaseEntity::getId).collect(Collectors.toSet());

        if (profissionalId != null) {
            return List.of(buscarProfissionalHabilitado(tenantId, profissionalId, servicos));
        }

        return profissionalService.listarAtivos(tenantId).stream()
                .filter(profissional -> realizaTodos(profissional, idsDosServicos))
                .toList();
    }

    /**
     * O profissional existe no negocio, esta ativo e realiza TODOS os servicos pedidos. Compartilhado com o
     * cadastro pelo painel (AgendamentoService), que ignora o expediente mas nao estas regras.
     */
    Profissional buscarProfissionalHabilitado(Long tenantId, Long profissionalId, List<ServiceOffering> servicos) {
        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        if (!profissional.isAtivo()) {
            throw new RegraDeNegocioException("Este profissional nao esta disponivel para agendamento.");
        }
        Set<Long> idsDosServicos = servicos.stream().map(BaseEntity::getId).collect(Collectors.toSet());
        if (!realizaTodos(profissional, idsDosServicos)) {
            throw new RegraDeNegocioException("Este profissional nao realiza todos os servicos escolhidos.");
        }
        return profissional;
    }

    private boolean realizaTodos(Profissional profissional, Set<Long> idsDosServicos) {
        Set<Long> realizados = profissional.getServicos().stream()
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());
        return realizados.containsAll(idsDosServicos);
    }

}
