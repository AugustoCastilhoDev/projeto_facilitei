package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalResolver;
import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Como o negocio recebe o sinal: Pix na chave propria (sem conta em gateway) e o WhatsApp do dono, para
 * onde vai o pedido de confirmacao de cada Pix. A chave da Asaas continua em /asaas-config.
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}")
@RequiredArgsConstructor
public class RecebimentoAdminController {

    private final TenantService tenantService;
    private final CobrancaSinalResolver cobrancaSinalResolver;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping("/recebimento")
    public RecebimentoResponse consultar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return resposta(tenantService.buscarPorId(tenantId));
    }

    @PutMapping("/recebimento/pix-proprio")
    public RecebimentoResponse configurarPixProprio(@PathVariable Long tenantId, @Valid @RequestBody PixProprioRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return resposta(tenantService.configurarPixProprio(
                tenantId, request.tipoChave(), request.chave(), request.nomeRecebedor(), request.cidadeRecebedor()));
    }

    @DeleteMapping("/recebimento/pix-proprio")
    public RecebimentoResponse removerPixProprio(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return resposta(tenantService.removerPixProprio(tenantId));
    }

    @PutMapping("/telefone-dono")
    public RecebimentoResponse atualizarTelefoneDono(@PathVariable Long tenantId, @Valid @RequestBody TelefoneDonoRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return resposta(tenantService.atualizarTelefoneDono(tenantId, request.telefone()));
    }

    private RecebimentoResponse resposta(Tenant tenant) {
        MeioCobranca meioAtivo = cobrancaSinalResolver.meioAtivo(tenant).map(meio -> meio.meio()).orElse(null);
        return RecebimentoResponse.de(tenant, meioAtivo);
    }

}
