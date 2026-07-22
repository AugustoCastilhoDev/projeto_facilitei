package com.castilhodigital.facilitei.professional;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final ServiceOfferingService serviceOfferingService;

    @Transactional
    public Profissional criar(Tenant tenant, String nome, LocalTime horarioAbertura, LocalTime horarioFechamento,
                               List<Long> servicoIds) {
        validarHorario(horarioAbertura, horarioFechamento);

        Profissional profissional = new Profissional();
        profissional.setTenant(tenant);
        profissional.setNome(nome);
        profissional.setHorarioAbertura(horarioAbertura);
        profissional.setHorarioFechamento(horarioFechamento);
        profissional.setAtivo(true);
        profissional.setServicos(resolverServicos(tenant.getId(), servicoIds));
        return profissionalRepository.save(profissional);
    }

    @Transactional
    public Profissional atualizar(Long tenantId, Long profissionalId, String nome, LocalTime horarioAbertura,
                                   LocalTime horarioFechamento, List<Long> servicoIds) {
        validarHorario(horarioAbertura, horarioFechamento);

        Profissional profissional = buscarPorIdETenant(tenantId, profissionalId);
        profissional.setNome(nome);
        profissional.setHorarioAbertura(horarioAbertura);
        profissional.setHorarioFechamento(horarioFechamento);
        profissional.setServicos(resolverServicos(tenantId, servicoIds));
        return profissional;
    }

    @Transactional
    public void desativar(Long tenantId, Long profissionalId) {
        buscarPorIdETenant(tenantId, profissionalId).setAtivo(false);
    }

    @Transactional
    public void ativar(Long tenantId, Long profissionalId) {
        buscarPorIdETenant(tenantId, profissionalId).setAtivo(true);
    }

    @Transactional(readOnly = true)
    public List<Profissional> listarTodos(Long tenantId) {
        return profissionalRepository.findByTenantIdOrderByNome(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Profissional> listarAtivos(Long tenantId) {
        return profissionalRepository.findByTenantIdAndAtivoTrueOrderByNome(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Profissional> listarAtivosPorServico(Long tenantId, Long serviceId) {
        return profissionalRepository.findByTenantIdAndServicosIdAndAtivoTrueOrderByNome(tenantId, serviceId);
    }

    @Transactional(readOnly = true)
    public Profissional buscarPorIdETenant(Long tenantId, Long profissionalId) {
        return profissionalRepository.findByIdAndTenantId(profissionalId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Profissional nao encontrado (id=" + profissionalId + ") para este tenant."));
    }

    private void validarHorario(LocalTime horarioAbertura, LocalTime horarioFechamento) {
        if (horarioAbertura == null || horarioFechamento == null || !horarioAbertura.isBefore(horarioFechamento)) {
            throw new RegraDeNegocioException("Horario de abertura deve ser anterior ao horario de fechamento.");
        }
    }

    private Set<ServiceOffering> resolverServicos(Long tenantId, List<Long> servicoIds) {
        Set<ServiceOffering> servicos = new HashSet<>();
        for (Long servicoId : servicoIds) {
            servicos.add(serviceOfferingService.buscarPorIdETenant(tenantId, servicoId));
        }
        return servicos;
    }

}
