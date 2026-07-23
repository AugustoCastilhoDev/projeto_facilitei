package com.castilhodigital.facilitei.billing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssinaturaAsaasWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AssinaturaAsaasWebhookControllerTest.TestConfig.class)
class AssinaturaAsaasWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssinaturaService assinaturaService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AsaasProperties asaasProperties() {
            return new AsaasProperties("https://api-sandbox.asaas.com/v3", "chave-plataforma", "segredo-teste");
        }
    }

    @Test
    void eventoPagamentoConfirmadoComTokenValidoChamaConfirmarPagamento() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_assinatura_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas-assinatura")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(assinaturaService).confirmarPagamento("pay_assinatura_1");
    }

    @Test
    void eventoPagamentoVencidoChamaMarcarComoVencida() throws Exception {
        String body = "{\"event\":\"PAYMENT_OVERDUE\",\"payment\":{\"id\":\"pay_assinatura_1\",\"status\":\"OVERDUE\"}}";

        mockMvc.perform(post("/api/webhooks/asaas-assinatura")
                        .header("asaas-access-token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(assinaturaService).marcarComoVencida("pay_assinatura_1");
    }

    @Test
    void eventoComTokenInvalidoRetorna401ENaoConfirma() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_assinatura_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas-assinatura")
                        .header("asaas-access-token", "token-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(assinaturaService, never()).confirmarPagamento(any());
    }

    @Test
    void eventoSemTokenRetorna401() throws Exception {
        String body = "{\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_assinatura_1\",\"status\":\"CONFIRMED\"}}";

        mockMvc.perform(post("/api/webhooks/asaas-assinatura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

}
