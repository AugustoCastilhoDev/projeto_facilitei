package com.castilhodigital.facilitei.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlotGenerationServiceTest {

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @Mock
    private ProfissionalService profissionalService;

    @Mock
    private SlotRepository slotRepository;

    private SlotGenerationService slotGenerationService;

    @BeforeEach
    void setUp() {
        slotGenerationService = new SlotGenerationService(serviceOfferingService, profissionalService, slotRepository);
    }

    private ServiceOffering servico(Tenant tenant, boolean ativo) {
        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setDuracaoMin(30);
        service.setAtivo(ativo);
        ReflectionTestUtils.setField(service, "id", 9L);
        return service;
    }

    private Profissional profissional(LocalTime abertura, LocalTime fechamento, boolean ativo, Set<ServiceOffering> servicos) {
        Profissional profissional = new Profissional();
        profissional.setHorarioAbertura(abertura);
        profissional.setHorarioFechamento(fechamento);
        profissional.setAtivo(ativo);
        profissional.setServicos(servicos);
        ReflectionTestUtils.setField(profissional, "id", 5L);
        return profissional;
    }

    @Test
    void gerarSlotsParaServicoDesativadoLancaExcecao() {
        Tenant tenant = new Tenant();
        ServiceOffering service = servico(tenant, false);

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);

        assertThatThrownBy(() -> slotGenerationService.gerarSlotsParaData(1L, 5L, 9L, LocalDate.of(2026, 7, 22)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    void gerarSlotsParaProfissionalDesativadoLancaExcecao() {
        Tenant tenant = new Tenant();
        ServiceOffering service = servico(tenant, true);
        Profissional profissional = profissional(LocalTime.of(9, 0), LocalTime.of(18, 0), false, Set.of(service));

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);
        when(profissionalService.buscarPorIdETenant(1L, 5L)).thenReturn(profissional);

        assertThatThrownBy(() -> slotGenerationService.gerarSlotsParaData(1L, 5L, 9L, LocalDate.of(2026, 7, 22)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    void gerarSlotsParaProfissionalQueNaoRealizaOServicoLancaExcecao() {
        Tenant tenant = new Tenant();
        ServiceOffering service = servico(tenant, true);
        Profissional profissional = profissional(LocalTime.of(9, 0), LocalTime.of(18, 0), true, Set.of());

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);
        when(profissionalService.buscarPorIdETenant(1L, 5L)).thenReturn(profissional);

        assertThatThrownBy(() -> slotGenerationService.gerarSlotsParaData(1L, 5L, 9L, LocalDate.of(2026, 7, 22)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nao realiza");
    }

    @Test
    void gerarSlotsParaServicoAtivoFunciona() {
        Tenant tenant = new Tenant();
        ServiceOffering service = servico(tenant, true);
        Profissional profissional = profissional(LocalTime.of(9, 0), LocalTime.of(10, 0), true, Set.of(service));

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);
        when(profissionalService.buscarPorIdETenant(1L, 5L)).thenReturn(profissional);
        when(slotRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var slots = slotGenerationService.gerarSlotsParaData(1L, 5L, 9L, LocalDate.of(2026, 7, 22));

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).getProfissional()).isSameAs(profissional);
    }

}
