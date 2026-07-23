package com.castilhodigital.facilitei.scheduling;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.booking.BookingService;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import com.castilhodigital.facilitei.professional.Profissional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SlotAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class SlotAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlotGenerationService slotGenerationService;

    @MockitoBean
    private SlotService slotService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    private Slot novoSlot(Long id) {
        ServiceOffering service = new ServiceOffering();
        service.setNome("Corte");
        ReflectionTestUtils.setField(service, "id", 4L);

        Profissional profissional = new Profissional();
        profissional.setNome("Joana");
        ReflectionTestUtils.setField(profissional, "id", 6L);

        Slot slot = new Slot();
        slot.setService(service);
        slot.setProfissional(profissional);
        slot.setDataHora(LocalDate.now().plusDays(1).atTime(LocalTime.of(9, 0)).atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime());
        slot.setStatus(SlotStatus.DISPONIVEL);
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    @Test
    void gerarSlotsRetorna201() throws Exception {
        LocalDate amanha = LocalDate.now().plusDays(1);
        when(slotGenerationService.gerarSlotsParaData(1L, 6L, 4L, amanha)).thenReturn(List.of(novoSlot(10L)));

        mockMvc.perform(post("/api/admin/tenants/1/services/4/slots/gerar")
                        .param("profissionalId", "6")
                        .param("data", amanha.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void listarAgendaRetornaTodosOsStatus() throws Exception {
        LocalDate amanha = LocalDate.now().plusDays(1);
        when(slotService.listarAgendaPorTenant(1L, null, amanha, amanha)).thenReturn(List.of(novoSlot(11L)));
        when(bookingService.buscarPorSlotIds(List.of(11L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/admin/tenants/1/slots").param("inicio", amanha.toString()).param("fim", amanha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    @Test
    void acessoATenantDeOutroDonoRetorna403() throws Exception {
        LocalDate amanha = LocalDate.now().plusDays(1);
        doThrow(new AcessoNegadoException("Voce nao tem acesso aos dados deste tenant."))
                .when(tenantSecurityGuard).verificarAcessoAoTenant(2L);

        mockMvc.perform(get("/api/admin/tenants/2/slots").param("inicio", amanha.toString()).param("fim", amanha.toString()))
                .andExpect(status().isForbidden());
    }

}
