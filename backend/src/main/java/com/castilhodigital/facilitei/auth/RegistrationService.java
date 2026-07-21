package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import com.castilhodigital.facilitei.user.User;
import com.castilhodigital.facilitei.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o onboarding: cria o tenant e o usuario admin na mesma
 * transacao, para nao deixar um tenant orfao (sem usuario) se o cadastro
 * do admin falhar (ex.: email duplicado).
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final TenantService tenantService;
    private final UserService userService;

    @Transactional
    public RegistrarTenantResponse registrarTenantEAdmin(RegistrarTenantRequest request) {
        Tenant tenant = tenantService.registrar(
                request.nomeNegocio(), request.slug(), request.horarioAbertura(), request.horarioFechamento());

        User admin = userService.registrarAdmin(tenant, request.emailAdmin(), request.senhaAdmin());

        return new RegistrarTenantResponse(tenant.getId(), tenant.getSlug(), admin.getId(), admin.getEmail());
    }

}
