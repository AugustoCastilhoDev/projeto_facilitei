package com.castilhodigital.facilitei.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private TenantService tenantService;

    private SlotService slotService;

    private Tenant tenant;
    private Profissional profissional1;
    private Profissional profissional2;

    @BeforeEach
    void setUp() {
        slotService = new SlotService(slotRepository, tenantService);
        tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
        profissional1 = profissional(10L);
        profissional2 = profissional(20L);
    }

    private Profissional profissional(Long id) {
        Profissional profissional = new Profissional();
        profissional.setTenant(tenant);
        ReflectionTestUtils.setField(profissional, "id", id);
        return profissional;
    }

    private ServiceOffering servico(int duracaoMin) {
        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setDuracaoMin(duracaoMin);
        service.setPreco(new BigDecimal("50.00"));
        service.setSinalPercentual(new BigDecimal("50.00"));
        service.setAtivo(true);
        return service;
    }

    private Slot slot(Long id, ServiceOffering service, Profissional profissional, OffsetDateTime dataHora, SlotStatus status) {
        Slot slot = new Slot();
        slot.setTenant(tenant);
        slot.setService(service);
        slot.setProfissional(profissional);
        slot.setDataHora(dataHora);
        slot.setStatus(status);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    @Test
    void reservarComHorarioSobrepostoPeloMesmoProfissionalLancaExcecao() {
        OffsetDateTime horario = OffsetDateTime.now(ZONE_ID).withHour(9).withMinute(30).withSecond(0).withNano(0);

        Slot candidato = slot(1L, servico(30), profissional1, horario, SlotStatus.DISPONIVEL);
        Slot ocupadoDeOutroServico = slot(2L, servico(30), profissional1, horario, SlotStatus.RESERVADO);

        when(slotRepository.findById(1L)).thenReturn(java.util.Optional.of(candidato));
        when(slotRepository.findOcupadosNoIntervalo(eq(10L), any(), any(), any()))
                .thenReturn(List.of(ocupadoDeOutroServico));

        assertThatThrownBy(() -> slotService.reservar(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("conflita");
    }

    @Test
    void reservarSemConflitoDeHorarioFunciona() {
        OffsetDateTime horario = OffsetDateTime.now(ZONE_ID).withHour(9).withMinute(30).withSecond(0).withNano(0);
        OffsetDateTime outroHorario = horario.plusHours(2);

        Slot candidato = slot(1L, servico(30), profissional1, horario, SlotStatus.DISPONIVEL);
        Slot ocupadoSemSobreposicao = slot(2L, servico(30), profissional1, outroHorario, SlotStatus.RESERVADO);

        when(slotRepository.findById(1L)).thenReturn(java.util.Optional.of(candidato));
        when(slotRepository.findOcupadosNoIntervalo(eq(10L), any(), any(), any()))
                .thenReturn(List.of(ocupadoSemSobreposicao));

        Slot resultado = slotService.reservar(1L);

        assertThat(resultado.getStatus()).isEqualTo(SlotStatus.RESERVADO);
    }

    @Test
    void listarDisponiveisPorSlugExcluiSlotSobrepostoPeloMesmoProfissional() {
        LocalDate data = LocalDate.now(ZONE_ID);
        OffsetDateTime horario = data.atStartOfDay(ZONE_ID).toOffsetDateTime().withHour(9).withMinute(30);

        Slot disponivelConflitante = slot(1L, servico(30), profissional1, horario, SlotStatus.DISPONIVEL);
        Slot disponivelLivre = slot(3L, servico(30), profissional1, horario.plusHours(3), SlotStatus.DISPONIVEL);
        Slot ocupadoDeOutroServico = slot(2L, servico(30), profissional1, horario, SlotStatus.RESERVADO);

        when(tenantService.buscarPorSlug("barbearia-teste")).thenReturn(tenant);
        when(slotRepository.findDisponiveisComServico(eq(1L), eq(SlotStatus.DISPONIVEL), any(), any()))
                .thenReturn(List.of(disponivelConflitante, disponivelLivre));
        when(slotRepository.findOcupadosNoIntervalo(eq(10L), any(), any(), any()))
                .thenReturn(List.of(ocupadoDeOutroServico));

        List<Slot> resultado = slotService.listarDisponiveisPorSlug("barbearia-teste", data);

        assertThat(resultado).containsExactly(disponivelLivre);
    }

    @Test
    void listarDisponiveisPorSlugNaoExcluiSlotDeProfissionalDiferente() {
        LocalDate data = LocalDate.now(ZONE_ID);
        OffsetDateTime horario = data.atStartOfDay(ZONE_ID).toOffsetDateTime().withHour(9).withMinute(30);

        Slot disponivelDeOutroProfissional = slot(1L, servico(30), profissional2, horario, SlotStatus.DISPONIVEL);

        when(tenantService.buscarPorSlug("barbearia-teste")).thenReturn(tenant);
        when(slotRepository.findDisponiveisComServico(eq(1L), eq(SlotStatus.DISPONIVEL), any(), any()))
                .thenReturn(List.of(disponivelDeOutroProfissional));
        when(slotRepository.findOcupadosNoIntervalo(eq(20L), any(), any(), any()))
                .thenReturn(List.of());

        List<Slot> resultado = slotService.listarDisponiveisPorSlug("barbearia-teste", data);

        assertThat(resultado).containsExactly(disponivelDeOutroProfissional);
    }

}
