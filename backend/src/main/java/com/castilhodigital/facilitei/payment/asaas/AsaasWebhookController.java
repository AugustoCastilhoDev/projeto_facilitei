package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.booking.BookingService;
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
 * Recebe as notificacoes de pagamento do Asaas. Publico do ponto de vista do
 * Spring Security (nao ha JWT de usuario aqui - quem chama e o Asaas, nao um
 * cliente do sistema), mas autenticado via o header "asaas-access-token",
 * configurado no painel do Asaas com o mesmo valor de facilitei.asaas.webhook-token.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
public class AsaasWebhookController {

    private static final Set<String> EVENTOS_DE_PAGAMENTO_CONFIRMADO = Set.of("PAYMENT_CONFIRMED", "PAYMENT_RECEIVED");
    private static final Set<String> EVENTOS_DE_PAGAMENTO_VENCIDO = Set.of("PAYMENT_OVERDUE");

    private final BookingService bookingService;
    private final AsaasProperties asaasProperties;

    @PostMapping
    public ResponseEntity<Void> receberEvento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody AsaasWebhookEvent evento) {

        if (!tokenValido(tokenRecebido)) {
            log.warn("Webhook do Asaas recebido com token invalido.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (EVENTOS_DE_PAGAMENTO_CONFIRMADO.contains(evento.event()) && evento.payment() != null) {
            bookingService.confirmarPagamento(evento.payment().id());
        } else if (EVENTOS_DE_PAGAMENTO_VENCIDO.contains(evento.event()) && evento.payment() != null) {
            bookingService.marcarComoExpirado(evento.payment().id());
        }

        return ResponseEntity.ok().build();
    }

    private boolean tokenValido(String tokenRecebido) {
        if (tokenRecebido == null) {
            return false;
        }
        byte[] recebido = tokenRecebido.getBytes(StandardCharsets.UTF_8);
        byte[] esperado = asaasProperties.webhookToken().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recebido, esperado);
    }

}
