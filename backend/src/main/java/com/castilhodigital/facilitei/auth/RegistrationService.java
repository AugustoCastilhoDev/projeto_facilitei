package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import com.castilhodigital.facilitei.user.User;
import com.castilhodigital.facilitei.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o onboarding: cria o tenant, o usuario admin e um profissional
 * padrao na mesma transacao, para nao deixar um tenant orfao (sem usuario ou
 * sem nenhum profissional agendavel) se algum passo falhar (ex.: email
 * duplicado). O profissional padrao ainda nao realiza nenhum servico (o
 * dono ainda nao cadastrou nenhum) - ele vincula depois pela tela
 * "Profissionais" do painel.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String NOME_PROFISSIONAL_PADRAO = "Profissional 1";

    private final TenantService tenantService;
    private final UserService userService;
    private final ProfissionalService profissionalService;

    @Transactional
    public RegistrarTenantResponse registrarTenantEAdmin(RegistrarTenantRequest request) {
        Tenant tenant = tenantService.registrar(request.nomeNegocio(), request.slug(), request.cpfCnpj(), request.plano());

        User admin = userService.registrarAdmin(tenant, request.emailAdmin(), request.senhaAdmin());

        profissionalService.criar(tenant, NOME_PROFISSIONAL_PADRAO,
                request.horarioAbertura(), request.horarioFechamento(), List.of());

        return new RegistrarTenantResponse(tenant.getId(), tenant.getSlug(), admin.getId(), admin.getEmail());
    }

}
