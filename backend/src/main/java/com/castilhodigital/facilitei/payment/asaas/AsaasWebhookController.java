package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.booking.BookingService;
import com.castilhodigital.facilitei.tenant.Tenant;
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
 * cliente do sistema).
 *
 * Como cada tenant tem sua PROPRIA conta Asaas (modelo BYOPP), nao ha mais
 * um unico segredo de webhook global: o tenant e identificado primeiro pelo
 * pagamento recebido (asaasPaymentId, sempre globalmente unico na Asaas), e
 * so entao o header "asaas-access-token" e comparado contra o
 * Tenant.asaasWebhookToken DAQUELE tenant especifico - gerado pela propria
 * plataforma quando o tenant configura sua chave (ver TenantAsaasConfigController).
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
public class AsaasWebhookController {

    private static final Set<String> EVENTOS_DE_PAGAMENTO_CONFIRMADO = Set.of("PAYMENT_CONFIRMED", "PAYMENT_RECEIVED");
    private static final Set<String> EVENTOS_DE_PAGAMENTO_VENCIDO = Set.of("PAYMENT_OVERDUE");

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<Void> receberEvento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody AsaasWebhookEvent evento) {

        if (evento.payment() == null) {
            return ResponseEntity.ok().build();
        }

        Tenant tenant = bookingService.buscarTenantPeloAsaasPaymentId(evento.payment().id());

        if (!tokenValido(tokenRecebido, tenant.getAsaasWebhookToken())) {
            log.warn("Webhook do Asaas recebido com token invalido para o tenant '{}'.", tenant.getSlug());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (EVENTOS_DE_PAGAMENTO_CONFIRMADO.contains(evento.event())) {
            bookingService.confirmarPagamento(evento.payment().id());
        } else if (EVENTOS_DE_PAGAMENTO_VENCIDO.contains(evento.event())) {
            bookingService.marcarComoExpirado(evento.payment().id());
        }

        return ResponseEntity.ok().build();
    }

    private boolean tokenValido(String tokenRecebido, String tokenEsperado) {
        if (tokenRecebido == null || tokenEsperado == null) {
            return false;
        }
        byte[] recebido = tokenRecebido.getBytes(StandardCharsets.UTF_8);
        byte[] esperado = tokenEsperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recebido, esperado);
    }

}
