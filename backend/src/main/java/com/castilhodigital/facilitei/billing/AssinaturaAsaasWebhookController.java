package com.castilhodigital.facilitei.billing;

import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import com.castilhodigital.facilitei.payment.asaas.AsaasWebhookEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe as notificacoes de pagamento da conta Asaas da PROPRIA plataforma
 * (cobranca da assinatura). Diferente de AsaasWebhookController (pacote
 * payment.asaas, que autentica por token especifico de cada tenant no
 * modelo BYOPP), aqui existe uma unica conta/token de plataforma - nao faz
 * sentido reaproveitar o mecanismo por-tenant para isso.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/asaas-assinatura")
@RequiredArgsConstructor
public class AssinaturaAsaasWebhookController {

    private static final Set<String> EVENTOS_DE_PAGAMENTO_CONFIRMADO = Set.of("PAYMENT_CONFIRMED", "PAYMENT_RECEIVED");
    private static final Set<String> EVENTOS_DE_PAGAMENTO_VENCIDO = Set.of("PAYMENT_OVERDUE");

    private final AssinaturaService assinaturaService;
    private final AsaasProperties asaasProperties;

    @PostMapping
    public ResponseEntity<Void> receberEvento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody AsaasWebhookEvent evento) {

        if (!tokenValido(tokenRecebido, asaasProperties.webhookToken())) {
            log.warn("Webhook da assinatura recebido com token invalido.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (evento.payment() == null) {
            return ResponseEntity.ok().build();
        }

        if (EVENTOS_DE_PAGAMENTO_CONFIRMADO.contains(evento.event())) {
            assinaturaService.confirmarPagamento(evento.payment().id());
        } else if (EVENTOS_DE_PAGAMENTO_VENCIDO.contains(evento.event())) {
            assinaturaService.marcarComoVencida(evento.payment().id());
        }

        return ResponseEntity.ok().build();
    }

    private boolean tokenValido(String tokenRecebido, String tokenEsperado) {
        if (tokenRecebido == null || tokenEsperado == null || tokenEsperado.isBlank()) {
            return false;
        }
        byte[] recebido = tokenRecebido.getBytes(StandardCharsets.UTF_8);
        byte[] esperado = tokenEsperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recebido, esperado);
    }

}
