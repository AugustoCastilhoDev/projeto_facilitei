package com.castilhodigital.facilitei.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint publico (sem autenticacao) com os dados basicos do negocio, usado pela pagina de agendamento do cliente final. */
@RestController
@RequestMapping("/api/public/tenants/{slug}")
@RequiredArgsConstructor
public class PublicTenantController {

    private final TenantService tenantService;

    @GetMapping
    public PublicTenantResponse buscar(@PathVariable String slug) {
        return PublicTenantResponse.from(tenantService.buscarPorSlug(slug));
    }

}
