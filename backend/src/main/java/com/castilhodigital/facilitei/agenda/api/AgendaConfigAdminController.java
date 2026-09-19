package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendaConfigService;
import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fuso, grade de horarios, antecedencia minima e horizonte da agenda do negocio. */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/agenda-config")
@RequiredArgsConstructor
public class AgendaConfigAdminController {

    private final AgendaConfigService agendaConfigService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public AgendaConfigDto buscar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendaConfigDto.de(agendaConfigService.buscar(tenantId));
    }

    @PutMapping
    public AgendaConfigDto atualizar(@PathVariable Long tenantId, @Valid @RequestBody AgendaConfigDto request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendaConfigDto.de(agendaConfigService.atualizar(tenantId, request.paraConfig()));
    }

}
