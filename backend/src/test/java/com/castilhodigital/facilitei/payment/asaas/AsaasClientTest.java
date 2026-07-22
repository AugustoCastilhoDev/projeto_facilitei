package com.castilhodigital.facilitei.payment.asaas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Testa o AsaasClient contra um servidor HTTP mockado (MockRestServiceServer),
 * sem depender de credenciais reais do Asaas - o que este projeto nao tem
 * neste ambiente. Confere que a URL, os headers (inclusive o access_token
 * por chamada, ja que cada tenant usa a propria chave - modelo BYOPP) e o
 * corpo das requisicoes batem exatamente com o documentado em docs.asaas.com.
 */
class AsaasClientTest {

    private static final String BASE_URL = "https://api-sandbox.asaas.com/v3";
    private static final String API_KEY = "chave-do-tenant-teste";

    private MockRestServiceServer mockServer;
    private AsaasClient asaasClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.createServer(builder);
        asaasClient = new AsaasClient(builder.build());
    }

    @Test
    void criarClienteEnviaNomeTelefoneEParseiaId() {
        mockServer.expect(requestTo(BASE_URL + "/customers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("access_token", API_KEY))
                .andExpect(jsonPath("$.name").value("Cliente Teste"))
                .andExpect(jsonPath("$.mobilePhone").value("+5511999999999"))
                .andRespond(withSuccess("{\"id\":\"cus_123\"}", MediaType.APPLICATION_JSON));

        String customerId = asaasClient.criarCliente(API_KEY, "Cliente Teste", "+5511999999999", null);

        assertThat(customerId).isEqualTo("cus_123");
        mockServer.verify();
    }

    @Test
    void criarCobrancaPixEnviaBillingTypePixEValor() {
        mockServer.expect(requestTo(BASE_URL + "/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("access_token", API_KEY))
                .andExpect(jsonPath("$.customer").value("cus_123"))
                .andExpect(jsonPath("$.billingType").value("PIX"))
                .andExpect(jsonPath("$.value").value(25.0))
                .andExpect(jsonPath("$.externalReference").value("booking-5"))
                .andRespond(withSuccess("{\"id\":\"pay_123\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        AsaasPaymentResponse response = asaasClient.criarCobrancaPix(
                API_KEY, "cus_123", new BigDecimal("25.00"), LocalDate.now(), "booking-5");

        assertThat(response.id()).isEqualTo("pay_123");
        assertThat(response.status()).isEqualTo("PENDING");
        mockServer.verify();
    }

    @Test
    void buscarQrCodePixRetornaPayloadEImagemBase64() {
        mockServer.expect(requestTo(BASE_URL + "/payments/pay_123/pixQrCode"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("access_token", API_KEY))
                .andRespond(withSuccess(
                        "{\"encodedImage\":\"base64img\",\"payload\":\"00020126copiaecola\",\"expirationDate\":\"2026-07-23\"}",
                        MediaType.APPLICATION_JSON));

        AsaasPixQrCodeResponse response = asaasClient.buscarQrCodePix(API_KEY, "pay_123");

        assertThat(response.encodedImage()).isEqualTo("base64img");
        assertThat(response.payload()).isEqualTo("00020126copiaecola");
        mockServer.verify();
    }

    @Test
    void erroDoAsaasViraPaymentGatewayException() {
        mockServer.expect(requestTo(BASE_URL + "/customers"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"errors\":[{\"description\":\"nome invalido\"}]}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> asaasClient.criarCliente(API_KEY, "", "", null))
                .isInstanceOf(PaymentGatewayException.class);
    }

}
