package com.castilhodigital.facilitei.payment;

/**
 * Porta de gateway de pagamento, no mesmo espirito do NotificationService:
 * o dominio (BookingCheckoutService) pede uma cobranca Pix sem saber que o
 * provedor por tras e o Asaas - trocar de provedor no futuro nao deveria
 * exigir mudar nada fora do pacote payment.asaas.
 */
public interface PaymentGatewayService {

    /**
     * apiKey identifica DE QUEM e a cobranca: cada tenant tem sua propria
     * conta Asaas (modelo "traga sua propria conta de pagamento" - BYOPP),
     * entao a chave nao e fixa/global, precisa ser resolvida pelo chamador
     * (ver BookingCheckoutService) a partir do tenant dono do agendamento.
     */
    PixChargeResult criarCobrancaPix(String apiKey, PixChargeRequest request);

    /**
     * Rebusca o payload/QR Code de uma cobranca ja existente. Usado quando o
     * cliente recarrega a pagina de pagamento: o QR Code (imagem) nao e
     * persistido no Booking, entao precisa ser buscado de novo no gateway.
     */
    PixChargeResult buscarQrCodePix(String apiKey, String paymentId);

}
