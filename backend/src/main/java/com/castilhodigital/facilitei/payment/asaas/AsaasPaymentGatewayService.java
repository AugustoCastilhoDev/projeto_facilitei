package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsaasPaymentGatewayService implements PaymentGatewayService {

    /** Mesma simplificacao de fuso da etapa 3 (SlotGenerationService): MVP focado em negocios no Brasil. */
    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private final AsaasClient asaasClient;

    @Override
    public PixChargeResult criarCobrancaPix(PixChargeRequest request) {
        String customerId = asaasClient.criarCliente(
                request.clienteNome(), request.clienteTelefone(), request.clienteCpfCnpj());

        // vencimento amanha: da ao cliente uma janela de ~24h para pagar o
        // sinal via Pix antes da cobranca expirar.
        LocalDate vencimento = LocalDate.now(ZONE_ID).plusDays(1);
        AsaasPaymentResponse payment = asaasClient.criarCobrancaPix(
                customerId, request.valor(), vencimento, request.referenciaExterna());

        AsaasPixQrCodeResponse qrCode = asaasClient.buscarQrCodePix(payment.id());

        return new PixChargeResult(payment.id(), qrCode.payload(), qrCode.encodedImage());
    }

}
