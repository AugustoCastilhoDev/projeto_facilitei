package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint publico (sem autenticacao) usado pela pagina de agendamento do cliente final. */
@RestController
@RequestMapping("/api/public/tenants/{slug}/services")
@RequiredArgsConstructor
public class PublicServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;
    private final TenantService tenantService;

    @GetMapping
    public List<ServiceOfferingResponse> listarAtivos(@PathVariable String slug) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        return serviceOfferingService.listarAtivos(tenant.getId()).stream()
                .map(service -> ServiceOfferingResponse.from(service, List.of()))
                .toList();
    }

}
