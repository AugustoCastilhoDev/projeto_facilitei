package com.castilhodigital.facilitei.payment;

/** Falha na comunicacao com o provedor de pagamento externo. Mapeada para HTTP 502 (Bad Gateway). */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

}
