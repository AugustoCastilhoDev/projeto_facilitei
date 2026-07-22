package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Configuracao da propria conta Asaas de cada tenant (modelo "traga sua
 * propria conta de pagamento" - BYOPP): a cobranca Pix do sinal e criada
 * direto na conta Asaas do negocio, nao na da plataforma. Ver README/ROADMAP
 * para o desenho completo e o tutorial em docs/configurar-pagamentos.md.
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/asaas-config")
@RequiredArgsConstructor
public class TenantAsaasConfigController {

    private final TenantService tenantService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public AsaasConfigResponse consultar(@PathVariable Long tenantId, HttpServletRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Tenant tenant = tenantService.buscarPorId(tenantId);
        return AsaasConfigResponse.from(tenant, webhookUrl(request));
    }

    @PutMapping
    public AsaasConfigResponse atualizar(@PathVariable Long tenantId,
                                          @Valid @RequestBody AtualizarAsaasApiKeyRequest request,
                                          HttpServletRequest servletRequest) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Tenant tenant = tenantService.configurarAsaas(tenantId, request.apiKey());
        return AsaasConfigResponse.from(tenant, webhookUrl(servletRequest));
    }

    /**
     * Construida a partir da requisicao atual (nao de uma URL fixa em
     * config) para refletir o host correto tanto em dev (localhost) quanto
     * atras de um proxy reverso em producao, desde que
     * server.forward-headers-strategy esteja configurado.
     */
    private String webhookUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .replacePath("/api/webhooks/asaas")
                .replaceQuery(null)
                .toUriString();
    }

}
