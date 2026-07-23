package com.castilhodigital.facilitei.notification.myzap;

import com.castilhodigital.facilitei.notification.NotificationGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
class MyZapClient {

    private final RestClient myZapRestClient;

    void enviarTexto(String numero, String texto) {
        try {
            myZapRestClient.post()
                    .uri("/mensagens/texto")
                    .body(new MyZapEnviarTextoRequest(numero, texto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new NotificationGatewayException("Falha ao enviar mensagem de texto no MyZap: " + detalhes(ex), ex);
        }
    }

    /**
     * RestClientResponseException (HTTP 4xx/5xx) tem corpo de resposta para
     * depurar o motivo exato; falhas de rede/timeout (ResourceAccessException)
     * nao tem - so a mensagem da excecao mesmo.
     */
    private String detalhes(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseException) {
            return responseException.getResponseBodyAsString();
        }
        return ex.getMessage();
    }

}
