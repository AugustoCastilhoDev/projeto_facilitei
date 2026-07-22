package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Transactional
    public ServiceOffering criar(Tenant tenant, String nome, int duracaoMin, BigDecimal preco, BigDecimal sinalPercentual) {
        validar(duracaoMin, preco, sinalPercentual);

        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setNome(nome);
        service.setDuracaoMin(duracaoMin);
        service.setPreco(preco);
        service.setSinalPercentual(sinalPercentual);
        service.setAtivo(true);
        return serviceOfferingRepository.save(service);
    }

    @Transactional
    public ServiceOffering atualizar(Long tenantId, Long serviceId, String nome, int duracaoMin,
                                      BigDecimal preco, BigDecimal sinalPercentual) {
        validar(duracaoMin, preco, sinalPercentual);

        ServiceOffering service = buscarPorIdETenant(tenantId, serviceId);
        service.setNome(nome);
        service.setDuracaoMin(duracaoMin);
        service.setPreco(preco);
        service.setSinalPercentual(sinalPercentual);
        return service;
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
