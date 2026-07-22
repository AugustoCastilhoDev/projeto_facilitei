package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agenda do admin (dia/semana, todos os status) e geracao de slots.
 *
 * Mesma protecao do ServiceOfferingAdminController: TenantSecurityGuard
 * confere o tenantId do path contra o do JWT autenticado (etapa 5).
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}")
@RequiredArgsConstructor
public class SlotAdminController {

    private final SlotGenerationService slotGenerationService;
    private final SlotService slotService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @PostMapping("/services/{serviceId}/slots/gerar")
    public ResponseEntity<List<SlotResponse>> gerarSlots(@PathVariable Long tenantId,
                                                          @PathVariable Long serviceId,
                                                          @RequestParam Long profissionalId,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        List<Slot> criados = slotGenerationService.gerarSlotsParaData(tenantId, profissionalId, serviceId, data);
        List<SlotResponse> response = criados.stream().map(SlotResponse::from).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/slots")
    public List<SlotResponse> listarAgenda(@PathVariable Long tenantId,
                                            @RequestParam(required = false) Long profissionalId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return slotService.listarAgendaPorTenant(tenantId, profissionalId, inicio, fim).stream()
                .map(SlotResponse::from)
                .toList();
    }

}
