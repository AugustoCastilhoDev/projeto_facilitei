package com.castilhodigital.facilitei.payment.asaas;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.booking.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AsaasWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class AsaasWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AsaasProperties asaasProperties;

    @BeforeEach
    void setUp() {
        when(asaasProperties.webhookToken()).thenReturn("segredo-teste");
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
    void eventoComTokenInvalidoRetorna401ENaoChamaBooking() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "token-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
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
    void eventoIrrelevanteNaoChamaConfirmarPagamento() throws Exception {
        String body = "{\"event\":\"PAYMENT_OVERDUE\",\"payment\":{\"id\":\"pay_1\",\"status\":\"OVERDUE\"}}";

        mockMvc.perform(post("/api/webhooks/asaas")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verifyNoInteractions(bookingService);
    }

}
