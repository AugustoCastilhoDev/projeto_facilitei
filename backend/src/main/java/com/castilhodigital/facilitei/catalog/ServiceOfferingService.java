package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalRepository;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ProfissionalRepository profissionalRepository;

    @Transactional
    public ServiceOffering criar(Tenant tenant, String nome, int duracaoMin, BigDecimal preco,
                                  BigDecimal sinalPercentual, List<Long> profissionalIds) {
        validar(duracaoMin, preco, sinalPercentual);

        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setNome(nome);
        service.setDuracaoMin(duracaoMin);
        service.setPreco(preco);
        service.setSinalPercentual(sinalPercentual);
        service.setAtivo(true);
        service = serviceOfferingRepository.save(service);
        sincronizarProfissionais(tenant.getId(), service, profissionalIds);
        return service;
    }

    @Transactional
    public ServiceOffering atualizar(Long tenantId, Long serviceId, String nome, int duracaoMin,
                                      BigDecimal preco, BigDecimal sinalPercentual, List<Long> profissionalIds) {
        validar(duracaoMin, preco, sinalPercentual);

        ServiceOffering service = buscarPorIdETenant(tenantId, serviceId);
        service.setNome(nome);
        service.setDuracaoMin(duracaoMin);
        service.setPreco(preco);
        service.setSinalPercentual(sinalPercentual);
        sincronizarProfissionais(tenantId, service, profissionalIds);
        return service;
    }

    /**
     * Servico (catalog) nao e o owning side do @ManyToMany com Profissional
     * (esse e Profissional.servicos, no pacote professional) - entao vincular
     * pelo lado do Servico exige escrever na colecao de cada Profissional
     * afetado, dentro da mesma transacao do create/update do servico. Injeta
     * ProfissionalRepository (nao ProfissionalService) para nao fechar um
     * ciclo de bean, ja que ProfissionalService ja depende deste service.
     * Sem save()/saveAll() explicito - dirty checking do Hibernate cuida do
     * flush ao fim da transacao, mesmo padrao ja usado em atualizar() acima.
     */
    private void sincronizarProfissionais(Long tenantId, ServiceOffering service, List<Long> profissionalIdsDesejados) {
        List<Profissional> todos = profissionalRepository.findByTenantIdOrderByNome(tenantId);
        Set<Long> existentes = todos.stream().map(Profissional::getId).collect(Collectors.toSet());
        for (Long id : profissionalIdsDesejados) {
            if (!existentes.contains(id)) {
                throw new EntidadeNaoEncontradaException("Profissional nao encontrado (id=" + id + ") para este tenant.");
            }
        }

        Set<Long> desejados = new HashSet<>(profissionalIdsDesejados);
        for (Profissional p : todos) {
            boolean deveTer = desejados.contains(p.getId());
            boolean tem = p.getServicos().stream().anyMatch(s -> s.getId().equals(service.getId()));
            if (deveTer && !tem) {
                p.getServicos().add(service);
            } else if (!deveTer && tem) {
                p.getServicos().removeIf(s -> s.getId().equals(service.getId()));
            }
        }
    }

    @Transactional
    public void desativar(Long tenantId, Long serviceId) {
        ServiceOffering service = buscarPorIdETenant(tenantId, serviceId);
        service.setAtivo(false);
    }

    @Transactional
    public void ativar(Long tenantId, Long serviceId) {
        ServiceOffering service = buscarPorIdETenant(tenantId, serviceId);
        service.setAtivo(true);
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> listarTodos(Long tenantId) {
        return serviceOfferingRepository.findByTenantIdOrderByNome(tenantId);
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> listarAtivos(Long tenantId) {
        return serviceOfferingRepository.findByTenantIdAndAtivoTrueOrderByNome(tenantId);
    }

    @Transactional(readOnly = true)
    public ServiceOffering buscarPorIdETenant(Long tenantId, Long serviceId) {
        return serviceOfferingRepository.findByIdAndTenantId(serviceId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Servico nao encontrado (id=" + serviceId + ") para este tenant."));
    }

    private void validar(int duracaoMin, BigDecimal preco, BigDecimal sinalPercentual) {
        if (duracaoMin <= 0) {
            throw new RegraDeNegocioException("Duracao do servico deve ser maior que zero.");
        }
        if (preco == null || preco.signum() < 0) {
            throw new RegraDeNegocioException("Preco do servico nao pode ser negativo.");
        }
        if (sinalPercentual == null || sinalPercentual.signum() < 0 || sinalPercentual.compareTo(CEM) > 0) {
            throw new RegraDeNegocioException("Percentual de sinal deve estar entre 0 e 100.");
        }
    }

}
