package com.castilhodigital.facilitei.professional;

import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico (sem autenticacao) usado pela pagina de agendamento do
 * cliente final, para listar os profissionais que realizam um servico
 * especifico (etapa "escolher profissional", entre servico e horario).
 */
@RestController
@RequestMapping("/api/public/tenants/{slug}/profissionais")
@RequiredArgsConstructor
public class PublicProfissionalController {

    private final ProfissionalService profissionalService;
    private final TenantService tenantService;

    @GetMapping
    public List<ProfissionalResponse> listar(@PathVariable String slug, @RequestParam Long serviceId) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        return profissionalService.listarAtivosPorServico(tenant.getId(), serviceId).stream()
                .map(ProfissionalResponse::from)
                .toList();
    }

}
