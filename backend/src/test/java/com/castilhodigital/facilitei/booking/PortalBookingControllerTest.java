package com.castilhodigital.facilitei.booking;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.AuthenticatedClienteResolver;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.cliente.ClienteService;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PortalBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingCheckoutService bookingCheckoutService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private AuthenticatedClienteResolver authenticatedClienteResolver;

    private Cliente clienteAutenticado() {
        Cliente cliente = new Cliente();
        cliente.setTelefone("5563999999999");
        cliente.setNome("Maria");
        ReflectionTestUtils.setField(cliente, "id", 3L);
        return cliente;
    }

    @Test
    void criarComDadosValidosRetorna201() throws Exception {
        when(authenticatedClienteResolver.clienteIdAtual()).thenReturn(3L);
        Cliente cliente = clienteAutenticado();
        when(clienteService.buscarPorId(3L)).thenReturn(cliente);

        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia do Ze");
        tenant.setSlug("barbearia-do-ze");

        ServiceOffering service = new ServiceOffering();
        service.setNome("Corte");

        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 7L);
        slot.setTenant(tenant);
        slot.setService(service);
        slot.setDataHora(OffsetDateTime.parse("2026-08-01T10:00:00-03:00"));

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setCliente(cliente);
        booking.setClienteNome("Maria");
        booking.setClienteTelefone("5563999999999");
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        ReflectionTestUtils.setField(booking, "id", 42L);

        CheckoutResult resultado = new CheckoutResult(booking, "base64-fake");
        when(bookingCheckoutService.iniciarCheckout(7L, cliente, "Maria", "11111111111")).thenReturn(resultado);

        CriarReservaPortalRequest request = new CriarReservaPortalRequest(7L, "Maria", "11111111111");

        mockMvc.perform(post("/api/portal/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.tenantNome").value("Barbearia do Ze"))
                .andExpect(jsonPath("$.serviceNome").value("Corte"));
    }

    @Test
    void criarSemSlotIdRetorna400() throws Exception {
        CriarReservaPortalRequest request = new CriarReservaPortalRequest(null, "Maria", "11111111111");

        mockMvc.perform(post("/api/portal/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void minhasReservasRetornaHistoricoDoClienteAutenticado() throws Exception {
        when(authenticatedClienteResolver.clienteIdAtual()).thenReturn(3L);

        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia do Ze");
        tenant.setSlug("barbearia-do-ze");

        ServiceOffering service = new ServiceOffering();
        service.setNome("Corte");

        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 7L);
        slot.setTenant(tenant);
        slot.setService(service);
        slot.setDataHora(OffsetDateTime.parse("2026-08-01T10:00:00-03:00"));

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteNome("Maria");
        booking.setClienteTelefone("5563999999999");
        booking.setStatusPagamento(PaymentStatus.PAGO);
        ReflectionTestUtils.setField(booking, "id", 42L);

        when(bookingService.listarPorCliente(3L)).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/portal/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].tenantNome").value("Barbearia do Ze"))
                .andExpect(jsonPath("$[0].statusPagamento").value("PAGO"));
    }

}
