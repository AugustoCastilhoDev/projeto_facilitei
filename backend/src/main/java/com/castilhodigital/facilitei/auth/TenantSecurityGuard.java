package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fecha a brecha de IDOR documentada na etapa 4: os endpoints admin
 * continuam recebendo o tenantId pelo path (para manter a URL RESTful e
 * previsivel), mas agora todo acesso e validado contra o tenantId contido
 * no JWT do usuario autenticado - se nao baterem, 403. Defesa em
 * profundidade: nunca confiar so no que o cliente manda na URL.
 */
@Component
@RequiredArgsConstructor
public class TenantSecurityGuard {

    private final AuthenticatedTenantResolver authenticatedTenantResolver;

    public void verificarAcessoAoTenant(Long tenantIdSolicitado) {
        Long tenantIdAtual = authenticatedTenantResolver.tenantIdAtual();
        if (!tenantIdAtual.equals(tenantIdSolicitado)) {
            throw new AcessoNegadoException("Voce nao tem acesso aos dados deste tenant.");
        }
    }

}
