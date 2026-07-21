package com.castilhodigital.facilitei.payment;

/**
 * Porta de gateway de pagamento, no mesmo espirito do NotificationService:
 * o dominio (BookingCheckoutService) pede uma cobranca Pix sem saber que o
 * provedor por tras e o Asaas - trocar de provedor no futuro nao deveria
 * exigir mudar nada fora do pacote payment.asaas.
 */
public interface PaymentGatewayService {

    PixChargeResult criarCobrancaPix(PixChargeRequest request);

}
