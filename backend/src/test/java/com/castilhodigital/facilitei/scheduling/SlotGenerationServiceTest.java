package com.castilhodigital.facilitei.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotGenerationServiceTest {

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @Mock
    private SlotRepository slotRepository;

    private SlotGenerationService slotGenerationService;

    @BeforeEach
    void setUp() {
        slotGenerationService = new SlotGenerationService(serviceOfferingService, slotRepository);
    }

    @Test
    void gerarSlotsParaServicoDesativadoLancaExcecao() {
        Tenant tenant = new Tenant();
        tenant.setHorarioAbertura(LocalTime.of(9, 0));
        tenant.setHorarioFechamento(LocalTime.of(18, 0));

        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setDuracaoMin(30);
        service.setAtivo(false);

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);

        assertThatThrownBy(() -> slotGenerationService.gerarSlotsParaData(1L, 9L, LocalDate.of(2026, 7, 22)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    void gerarSlotsParaServicoAtivoFunciona() {
        Tenant tenant = new Tenant();
        tenant.setHorarioAbertura(LocalTime.of(9, 0));
        tenant.setHorarioFechamento(LocalTime.of(10, 0));

        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setDuracaoMin(30);
        service.setAtivo(true);

        when(serviceOfferingService.buscarPorIdETenant(1L, 9L)).thenReturn(service);
        when(slotRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var slots = slotGenerationService.gerarSlotsParaData(1L, 9L, LocalDate.of(2026, 7, 22));

        assertThat(slots).hasSize(2);
    }

}
