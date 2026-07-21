package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.scheduling.SlotService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico (sem autenticacao) que o cliente final usa para reservar
 * um horario. Reserva o slot, cria a cobranca Pix no Asaas (etapa 6) e
 * devolve o QR Code/payload para o cliente pagar o sinal. A confirmacao
 * definitiva chega depois via webhook (AsaasWebhookController).
 */
@RestController
@RequestMapping("/api/public/tenants/{slug}/bookings")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BookingCheckoutService bookingCheckoutService;
    private final SlotService slotService;
    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<BookingResponse> criar(@PathVariable String slug,
                                                  @Valid @RequestBody CriarBookingRequest request) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        // valida que o slot pertence a este tenant antes de reservar (ver SlotService.buscarPorIdETenant)
        slotService.buscarPorIdETenant(request.slotId(), tenant.getId());

        CheckoutResult resultado = bookingCheckoutService.iniciarCheckout(
                request.slotId(), request.clienteNome(), request.clienteTelefone(), request.clienteCpfCnpj());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(resultado));
    }

}
