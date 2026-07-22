package com.castilhodigital.facilitei.booking;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.scheduling.SlotService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PublicBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingCheckoutService bookingCheckoutService;

    @MockitoBean
    private SlotService slotService;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void criarComSlotDoProprioTenantRetorna201() throws Exception {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
        when(tenantService.buscarPorSlug("barbearia-do-ze")).thenReturn(tenant);

        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 7L);
        when(slotService.buscarPorIdETenant(7L, 1L)).thenReturn(slot);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteNome("Cliente Teste");
        booking.setClienteTelefone("+5511999999999");
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        booking.setAsaasPaymentId("pay_123");
        booking.setAsaasPixPayload("00020126...copiaecola");
        ReflectionTestUtils.setField(booking, "id", 42L);

        CheckoutResult checkoutResult = new CheckoutResult(booking, "base64-fake-qrcode");
        when(bookingCheckoutService.iniciarCheckout(7L, "Cliente Teste", "+5511999999999", "24971563792"))
                .thenReturn(checkoutResult);

        CriarBookingRequest request = new CriarBookingRequest(7L, "Cliente Teste", "+5511999999999", "24971563792");

        mockMvc.perform(post("/api/public/tenants/barbearia-do-ze/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.asaasPixQrCodeBase64").value("base64-fake-qrcode"));
    }

    @Test
    void criarComSlotDeOutroTenantRetorna404() throws Exception {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
        when(tenantService.buscarPorSlug("barbearia-do-ze")).thenReturn(tenant);

        when(slotService.buscarPorIdETenant(999L, 1L))
                .thenThrow(new EntidadeNaoEncontradaException("Slot nao encontrado (id=999)."));

        CriarBookingRequest request = new CriarBookingRequest(999L, "Cliente Teste", "+5511999999999", "24971563792");

        mockMvc.perform(post("/api/public/tenants/barbearia-do-ze/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarSemNomeClienteRetorna400() throws Exception {
        CriarBookingRequest request = new CriarBookingRequest(7L, "", "+5511999999999", "24971563792");

        mockMvc.perform(post("/api/public/tenants/barbearia-do-ze/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarSemCpfCnpjRetorna400() throws Exception {
        // a Asaas exige CPF/CNPJ para gerar a cobranca (confirmado testando
        // contra o sandbox real) - por isso e obrigatorio aqui tambem.
        CriarBookingRequest request = new CriarBookingRequest(7L, "Cliente Teste", "+5511999999999", "");

        mockMvc.perform(post("/api/public/tenants/barbearia-do-ze/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.clienteCpfCnpj").exists());
    }

    @Test
    void buscarStatusDaReservaRetornaPagamentoAtual() throws Exception {
        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 7L);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteNome("Cliente Teste");
        booking.setClienteTelefone("+5511999999999");
        booking.setStatusPagamento(PaymentStatus.PAGO);
        booking.setAsaasPaymentId("pay_123");
        booking.setAsaasPixPayload("00020126...copiaecola");
        ReflectionTestUtils.setField(booking, "id", 42L);

        when(bookingCheckoutService.buscarStatusAtual(42L, "barbearia-do-ze"))
                .thenReturn(new CheckoutResult(booking, null));

        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze/bookings/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("PAGO"));
    }

    @Test
    void buscarStatusDeReservaDeOutroTenantRetorna404() throws Exception {
        when(bookingCheckoutService.buscarStatusAtual(42L, "outro-tenant"))
                .thenThrow(new EntidadeNaoEncontradaException("Reserva nao encontrada (id=42)."));

        mockMvc.perform(get("/api/public/tenants/outro-tenant/bookings/42"))
                .andExpect(status().isNotFound());
    }

}
