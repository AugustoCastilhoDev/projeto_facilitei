package com.castilhodigital.facilitei.payment.asaas;

import com.castilhodigital.facilitei.agenda.AgendamentoPagamentoService;
import com.castilhodigital.facilitei.booking.BookingService;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
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
 *
 * Pagamento que nao e de nenhuma reserva nossa responde 200 e e ignorado: na conta
 * Asaas do proprio negocio entram cobrancas que nada tem a ver com o Facilitei, e
 * um erro aqui faria a Asaas pausar a fila de sincronizacao do webhook (falhas
 * seguidas), deixando de entregar as confirmacoes de sinal que importam.
 *
 * Durante a migracao para a nova agenda convivem dois caminhos: agendamentos
 * (novo) e bookings (antigo, removido na fase 2e). A cobranca e procurada primeiro
 * na agenda nova.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
public class AsaasWebhookController {

    private static final Set<String> EVENTOS_DE_PAGAMENTO_CONFIRMADO = Set.of("PAYMENT_CONFIRMED", "PAYMENT_RECEIVED");
    private static final Set<String> EVENTOS_DE_PAGAMENTO_VENCIDO = Set.of("PAYMENT_OVERDUE");

    private final BookingService bookingService;
    private final AgendamentoPagamentoService agendamentoPagamentoService;

    @PostMapping
    public ResponseEntity<Void> receberEvento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody AsaasWebhookEvent evento) {

        if (evento.payment() == null) {
            return ResponseEntity.ok().build();
        }
        String paymentId = evento.payment().id();

        Optional<Tenant> donoNaAgendaNova = agendamentoPagamentoService.buscarTenantDaCobranca(paymentId);
        Optional<Tenant> dono = donoNaAgendaNova.isPresent() ? donoNaAgendaNova : buscarDonoNaAgendaAntiga(paymentId);
        if (dono.isEmpty()) {
            log.info("Webhook do Asaas para o pagamento {} que nao e de nenhuma reserva - ignorado.", paymentId);
            return ResponseEntity.ok().build();
        }

        Tenant tenant = dono.get();
        if (!tokenValido(tokenRecebido, tenant.getAsaasWebhookToken())) {
            log.warn("Webhook do Asaas recebido com token invalido para o tenant '{}'.", tenant.getSlug());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (donoNaAgendaNova.isPresent()) {
            processarNaAgendaNova(evento.event(), paymentId);
        } else {
            processarNaAgendaAntiga(evento.event(), paymentId);
        }
        return ResponseEntity.ok().build();
    }

    private void processarNaAgendaNova(String tipoDoEvento, String paymentId) {
        // PAYMENT_OVERDUE nao precisa de tratamento: o prazo do agendamento (minutos) acaba muito antes do
        // vencimento da cobranca (dia seguinte), e a expiracao propria ja liberou o horario
        if (EVENTOS_DE_PAGAMENTO_CONFIRMADO.contains(tipoDoEvento)) {
            agendamentoPagamentoService.processarPagamentoRecebido(paymentId, Instant.now());
        }
    }

    private Optional<Tenant> buscarDonoNaAgendaAntiga(String paymentId) {
        try {
            return Optional.of(bookingService.buscarTenantPeloAsaasPaymentId(paymentId));
        } catch (EntidadeNaoEncontradaException ex) {
            return Optional.empty();
        }
    }

    private void processarNaAgendaAntiga(String tipoDoEvento, String paymentId) {
        if (EVENTOS_DE_PAGAMENTO_CONFIRMADO.contains(tipoDoEvento)) {
            bookingService.confirmarPagamento(paymentId);
        } else if (EVENTOS_DE_PAGAMENTO_VENCIDO.contains(tipoDoEvento)) {
            bookingService.marcarComoExpirado(paymentId);
        }
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
