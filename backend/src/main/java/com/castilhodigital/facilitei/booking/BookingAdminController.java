package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acoes do admin sobre uma reserva ja existente (comparecimento e
 * cancelamento) - mesmo formato de protecao dos demais controllers admin
 * (TenantSecurityGuard confere o tenantId do path contra o do JWT).
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/bookings")
@RequiredArgsConstructor
public class BookingAdminController {

    private final BookingService bookingService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @PatchMapping("/{bookingId}/comparecimento")
    public ResponseEntity<Void> marcarComparecimento(@PathVariable Long tenantId, @PathVariable Long bookingId,
                                                      @Valid @RequestBody MarcarComparecimentoRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        bookingService.marcarComparecimento(bookingId, tenantId, request.compareceu());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{bookingId}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long tenantId, @PathVariable Long bookingId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        bookingService.cancelar(bookingId, tenantId);
        return ResponseEntity.noContent().build();
    }

}
