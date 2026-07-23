package com.castilhodigital.facilitei.billing;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Status da assinatura do tenant com a plataforma e cancelamento - protegido pelo mesmo TenantSecurityGuard dos demais admin controllers. */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/assinatura")
@RequiredArgsConstructor
public class AssinaturaAdminController {

    private final AssinaturaService assinaturaService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public AssinaturaResponse consultar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return assinaturaService.consultarStatus(tenantId);
    }

    @PatchMapping("/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        assinaturaService.cancelar(tenantId);
        return ResponseEntity.noContent().build();
    }

}
