package com.castilhodigital.facilitei.booking;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(BookingAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    @Test
    void marcarComparecimentoRetorna204() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/bookings/9/comparecimento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarcarComparecimentoRequest(true))))
                .andExpect(status().isNoContent());

        Mockito.verify(bookingService).marcarComparecimento(9L, 1L, true);
    }

    @Test
    void marcarComparecimentoSemCorpoRetorna400() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/bookings/9/comparecimento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcarComparecimentoDeReservaNaoConfirmadaRetorna400() throws Exception {
        doThrow(new RegraDeNegocioException("So e possivel marcar comparecimento de uma reserva confirmada."))
                .when(bookingService).marcarComparecimento(9L, 1L, true);

        mockMvc.perform(patch("/api/admin/tenants/1/bookings/9/comparecimento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarcarComparecimentoRequest(true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarRetorna204() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/bookings/9/cancelar"))
                .andExpect(status().isNoContent());

        Mockito.verify(bookingService).cancelar(9L, 1L);
    }

    @Test
    void acessoATenantDeOutroDonoRetorna403() throws Exception {
        doThrow(new AcessoNegadoException("Voce nao tem acesso aos dados deste tenant."))
                .when(tenantSecurityGuard).verificarAcessoAoTenant(2L);

        mockMvc.perform(patch("/api/admin/tenants/2/bookings/9/cancelar"))
                .andExpect(status().isForbidden());
    }

}
