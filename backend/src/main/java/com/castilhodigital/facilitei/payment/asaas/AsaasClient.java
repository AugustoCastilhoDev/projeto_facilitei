package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
class AsaasClient {

    private final RestClient asaasRestClient;

    String criarCliente(String nome, String telefone, String cpfCnpj) {
        try {
            AsaasCustomerResponse response = asaasRestClient.post()
                    .uri("/customers")
                    .body(new AsaasCustomerRequest(nome, telefone, cpfCnpj))
                    .retrieve()
                    .body(AsaasCustomerResponse.class);
            return response.id();
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Falha ao criar cliente no Asaas: " + detalhes(ex), ex);
        }
    }

    AsaasPaymentResponse criarCobrancaPix(String customerId, BigDecimal valor, LocalDate vencimento, String referenciaExterna) {
        try {
            return asaasRestClient.post()
                    .uri("/payments")
                    .body(new AsaasPaymentRequest(customerId, "PIX", valor, vencimento, referenciaExterna))
                    .retrieve()
                    .body(AsaasPaymentResponse.class);
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Falha ao criar cobranca Pix no Asaas: " + detalhes(ex), ex);
        }
    }

    AsaasPixQrCodeResponse buscarQrCodePix(String paymentId) {
        try {
            return asaasRestClient.get()
                    .uri("/payments/{id}/pixQrCode", paymentId)
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
