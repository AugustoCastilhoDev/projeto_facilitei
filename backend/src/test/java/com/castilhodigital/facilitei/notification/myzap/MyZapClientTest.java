package com.castilhodigital.facilitei.notification.myzap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.castilhodigital.facilitei.notification.NotificationGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Testa o MyZapClient contra um servidor HTTP mockado (MockRestServiceServer),
 * sem depender de credenciais reais do MyZap. A chave fixa e um header
 * default do RestClient (nao passada por chamada, ja que e uma unica conta
 * da propria plataforma, diferente do modelo BYOPP da Asaas).
 */
class MyZapClientTest {

    private static final String BASE_URL = "https://api.myzap.net/api/v1";
    private static final String API_KEY = "mz_chave_de_teste";

    private MockRestServiceServer mockServer;
    private MyZapClient myZapClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + API_KEY);
        mockServer = MockRestServiceServer.createServer(builder);
        myZapClient = new MyZapClient(builder.build());
    }

    @Test
    void enviarTextoEnviaNumeroETextoComAutenticacao() {
        mockServer.expect(requestTo(BASE_URL + "/mensagens/texto"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.numero").value("5511999998888"))
                .andExpect(jsonPath("$.texto").value("Ola pelo MyZap"))
                .andRespond(withSuccess("{\"sucesso\":true}", MediaType.APPLICATION_JSON));

        myZapClient.enviarTexto("5511999998888", "Ola pelo MyZap");

        mockServer.verify();
    }

    @Test
    void erroDoMyZapViraNotificationGatewayException() {
        mockServer.expect(requestTo(BASE_URL + "/mensagens/texto"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"erro\":\"chave invalida\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> myZapClient.enviarTexto("5511999998888", "Ola"))
                .isInstanceOf(NotificationGatewayException.class);
    }

}
