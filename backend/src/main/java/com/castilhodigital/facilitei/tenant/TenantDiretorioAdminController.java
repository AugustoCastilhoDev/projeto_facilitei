package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tela do admin para o dono do negocio optar (ou nao) por aparecer no diretorio publico do portal do cliente. */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/diretorio")
@RequiredArgsConstructor
public class TenantDiretorioAdminController {

    private final TenantService tenantService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public DiretorioConfigResponse consultar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return DiretorioConfigResponse.from(tenantService.buscarPorId(tenantId));
    }

    @PutMapping
    public DiretorioConfigResponse atualizar(@PathVariable Long tenantId, @RequestBody AtualizarDiretorioRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Tenant tenant = tenantService.atualizarDiretorio(
                tenantId, request.categoria(), request.cidade(), request.descricao(), request.visivelNoDiretorio());
        return DiretorioConfigResponse.from(tenant);
    }

}
