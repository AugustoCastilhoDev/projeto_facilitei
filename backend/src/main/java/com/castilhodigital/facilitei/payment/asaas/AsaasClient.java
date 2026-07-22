package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * apiKey e passada por chamada (nao e um header fixo no RestClient): cada
 * tenant tem sua propria conta Asaas (modelo BYOPP), entao a credencial
 * muda a cada requisicao dependendo de quem esta fazendo o checkout.
 */
@Component
@RequiredArgsConstructor
class AsaasClient {

    private static final String HEADER_ACCESS_TOKEN = "access_token";

    private final RestClient asaasRestClient;

    String criarCliente(String apiKey, String nome, String telefone, String cpfCnpj) {
        try {
            AsaasCustomerResponse response = asaasRestClient.post()
                    .uri("/customers")
                    .header(HEADER_ACCESS_TOKEN, apiKey)
                    .body(new AsaasCustomerRequest(nome, telefone, cpfCnpj))
                    .retrieve()
                    .body(AsaasCustomerResponse.class);
            return response.id();
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Falha ao criar cliente no Asaas: " + detalhes(ex), ex);
        }
    }

    AsaasPaymentResponse criarCobrancaPix(
            String apiKey, String customerId, BigDecimal valor, LocalDate vencimento, String referenciaExterna) {
        try {
            return asaasRestClient.post()
                    .uri("/payments")
                    .header(HEADER_ACCESS_TOKEN, apiKey)
                    .body(new AsaasPaymentRequest(customerId, "PIX", valor, vencimento, referenciaExterna))
                    .retrieve()
                    .body(AsaasPaymentResponse.class);
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Falha ao criar cobranca Pix no Asaas: " + detalhes(ex), ex);
        }
    }

    AsaasPixQrCodeResponse buscarQrCodePix(String apiKey, String paymentId) {
        try {
            return asaasRestClient.get()
                    .uri("/payments/{id}/pixQrCode", paymentId)
                    .header(HEADER_ACCESS_TOKEN, apiKey)
                    .retrieve()
                    .body(AsaasPixQrCodeResponse.class);
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Falha ao buscar QR Code Pix no Asaas: " + detalhes(ex), ex);
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
