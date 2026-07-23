package com.castilhodigital.facilitei.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalRepository;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

    @Mock
    private ServiceOfferingRepository serviceOfferingRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    private ServiceOfferingService serviceOfferingService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        serviceOfferingService = new ServiceOfferingService(serviceOfferingRepository, profissionalRepository);
        tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
    }

    private Profissional profissional(Long id, String nome) {
        Profissional profissional = new Profissional();
        profissional.setNome(nome);
        ReflectionTestUtils.setField(profissional, "id", id);
        return profissional;
    }

    @Test
    void criarVinculaProfissionaisSelecionados() {
        when(serviceOfferingRepository.save(ArgumentMatchers.any(ServiceOffering.class)))
                .thenAnswer(invocation -> {
                    ServiceOffering service = invocation.getArgument(0);
                    ReflectionTestUtils.setField(service, "id", 9L);
                    return service;
                });
        Profissional ana = profissional(7L, "Ana");
        Profissional bia = profissional(8L, "Bia");
        when(profissionalRepository.findByTenantIdOrderByNome(1L)).thenReturn(List.of(ana, bia));

        ServiceOffering servico = serviceOfferingService.criar(
                tenant, "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of(7L));

        assertThat(ana.getServicos()).contains(servico);
        assertThat(bia.getServicos()).doesNotContain(servico);
    }

    @Test
    void atualizarDesvinculaProfissionalRemovidoDaLista() {
        ServiceOffering servico = new ServiceOffering();
        ReflectionTestUtils.setField(servico, "id", 9L);
        when(serviceOfferingRepository.findByIdAndTenantId(9L, 1L)).thenReturn(Optional.of(servico));

        Profissional ana = profissional(7L, "Ana");
        ana.getServicos().add(servico);
        Profissional bia = profissional(8L, "Bia");
        when(profissionalRepository.findByTenantIdOrderByNome(1L)).thenReturn(List.of(ana, bia));

        serviceOfferingService.atualizar(
                1L, 9L, "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of(8L));

        assertThat(ana.getServicos()).doesNotContain(servico);
        assertThat(bia.getServicos()).contains(servico);
    }

    @Test
    void atualizarNaoAlteraVinculoDeOutroServico() {
        ServiceOffering servico = new ServiceOffering();
        ReflectionTestUtils.setField(servico, "id", 9L);
        when(serviceOfferingRepository.findByIdAndTenantId(9L, 1L)).thenReturn(Optional.of(servico));

        ServiceOffering outroServico = new ServiceOffering();
        ReflectionTestUtils.setField(outroServico, "id", 99L);
        Profissional ana = profissional(7L, "Ana");
        ana.getServicos().add(outroServico);
        when(profissionalRepository.findByTenantIdOrderByNome(1L)).thenReturn(List.of(ana));

        serviceOfferingService.atualizar(
                1L, 9L, "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of(7L));

        assertThat(ana.getServicos()).containsExactlyInAnyOrder(servico, outroServico);
    }

    @Test
    void atualizarComProfissionalInexistenteLancaExcecao() {
        ServiceOffering servico = new ServiceOffering();
        ReflectionTestUtils.setField(servico, "id", 9L);
        when(serviceOfferingRepository.findByIdAndTenantId(9L, 1L)).thenReturn(Optional.of(servico));
        when(profissionalRepository.findByTenantIdOrderByNome(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> serviceOfferingService.atualizar(
                1L, 9L, "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of(404L)))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void atualizarComDuracaoInvalidaLancaExcecao() {
        assertThatThrownBy(() -> serviceOfferingService.atualizar(
                1L, 9L, "Corte", 0, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

}
