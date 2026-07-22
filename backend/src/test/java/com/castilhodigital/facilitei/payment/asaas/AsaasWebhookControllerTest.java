package com.castilhodigital.facilitei.payment.asaas;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.booking.BookingService;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.tenant.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cada tenant tem sua propria chave/token Asaas (modelo BYOPP), entao o
 * webhook identifica o tenant pelo asaasPaymentId recebido ANTES de validar
 * o token - ver AsaasWebhookController.
 */
@WebMvcTest(AsaasWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class AsaasWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setNome("Negocio Teste");
        tenant.setSlug("negocio-teste");
        tenant.setAsaasWebhookToken("segredo-teste");

        when(bookingService.buscarTenantPeloAsaasPaymentId("pay_1")).thenReturn(tenant);
        when(bookingService.buscarTenantPeloAsaasPaymentId("pay_2")).thenReturn(tenant);
    }

    @Test
    void eventoPagamentoConfirmadoComTokenValidoChamaConfirmarPagamento() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(bookingService).confirmarPagamento("pay_1");
    }

    @Test
    void eventoPagamentoRecebidoTambemConfirma() throws Exception {
        String body = "{\"event\":\"PAYMENT_RECEIVED\",\"payment\":{\"id\":\"pay_2\",\"status\":\"RECEIVED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(bookingService).confirmarPagamento("pay_2");
    }

    @Test
    void eventoComTokenInvalidoRetorna401ENaoConfirmaPagamento() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "token-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(bookingService, never()).confirmarPagamento(any());
    }

    @Test
    void eventoSemTokenRetorna401() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eventoIrrelevanteComTokenValidoNaoConfirmaNemExpira() throws Exception {
        String body = "{\"event\":\"PAYMENT_CREATED\",\"payment\":{\"id\":\"pay_1\",\"status\":\"PENDING\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(bookingService, never()).confirmarPagamento(any());
        verify(bookingService, never()).marcarComoExpirado(any());
    }

    @Test
    void eventoPagamentoVencidoChamaMarcarComoExpirado() throws Exception {
        String body = "{\"event\":\"PAYMENT_OVERDUE\",\"payment\":{\"id\":\"pay_1\",\"status\":\"OVERDUE\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(bookingService).marcarComoExpirado("pay_1");
    }

    @Test
    void eventoParaPagamentoDesconhecidoRetorna404() throws Exception {
        when(bookingService.buscarTenantPeloAsaasPaymentId("pay_desconhecido"))
                .thenThrow(new EntidadeNaoEncontradaException("Nenhuma reserva encontrada."));
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_desconhecido\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

}
