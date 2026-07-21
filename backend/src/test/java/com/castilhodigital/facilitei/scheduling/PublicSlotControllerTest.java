package com.castilhodigital.facilitei.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicSlotController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlotService slotService;

    @Test
    void listarDisponiveisRetornaSlotsDoDia() throws Exception {
        ServiceOffering service = new ServiceOffering();
        service.setNome("Corte");
        ReflectionTestUtils.setField(service, "id", 3L);

        Slot slot = new Slot();
        slot.setService(service);
        slot.setDataHora(LocalDate.now().plusDays(1).atTime(LocalTime.of(9, 0)).atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime());
        slot.setStatus(SlotStatus.DISPONIVEL);
        ReflectionTestUtils.setField(slot, "id", 7L);

        LocalDate amanha = LocalDate.now().plusDays(1);
        when(slotService.listarDisponiveisPorSlug("barbearia-do-ze", amanha)).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze/slots").param("data", amanha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].serviceNome").value("Corte"))
                .andExpect(jsonPath("$[0].status").value("DISPONIVEL"));
    }

    @Test
    void listarSemParametroDataRetorna400() throws Exception {
        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze/slots"))
                .andExpect(status().isBadRequest());
    }

}
