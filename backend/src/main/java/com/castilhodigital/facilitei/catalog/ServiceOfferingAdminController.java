package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de servicos do painel admin.
 *
 * O tenantId continua vindo do path (URL RESTful e previsivel), mas desde a
 * etapa 5 todo acesso passa por TenantSecurityGuard, que confere se o
 * tenantId do path bate com o tenantId do JWT autenticado - fechando o IDOR
 * documentado na etapa 4 (OWASP API1:2023).
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/services")
@RequiredArgsConstructor
public class ServiceOfferingAdminController {

    private final ServiceOfferingService serviceOfferingService;
    private final ProfissionalService profissionalService;
    private final TenantService tenantService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public List<ServiceOfferingResponse> listar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        List<ServiceOffering> servicos = serviceOfferingService.listarTodos(tenantId);
        List<Profissional> profissionais = profissionalService.listarTodos(tenantId);
        return servicos.stream()
                .map(servico -> ServiceOfferingResponse.from(servico, profissionaisDoServico(profissionais, servico)))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingResponse> criar(@PathVariable Long tenantId,
                                                          @Valid @RequestBody ServiceOfferingRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Tenant tenant = tenantService.buscarPorId(tenantId);
        ServiceOffering service = serviceOfferingService.criar(tenant, request.nome(), request.duracaoMin(),
                request.preco(), request.sinalPercentual(), request.profissionalIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenantId, service));
    }

    @PutMapping("/{serviceId}")
    public ServiceOfferingResponse atualizar(@PathVariable Long tenantId, @PathVariable Long serviceId,
                                              @Valid @RequestBody ServiceOfferingRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        ServiceOffering service = serviceOfferingService.atualizar(tenantId, serviceId, request.nome(),
                request.duracaoMin(), request.preco(), request.sinalPercentual(), request.profissionalIds());
        return toResponse(tenantId, service);
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> desativar(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        serviceOfferingService.desativar(tenantId, serviceId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{serviceId}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long tenantId, @PathVariable Long serviceId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        serviceOfferingService.ativar(tenantId, serviceId);
        return ResponseEntity.noContent().build();
    }

    private ServiceOfferingResponse toResponse(Long tenantId, ServiceOffering service) {
        List<Profissional> profissionais = profissionalService.listarTodos(tenantId);
        return ServiceOfferingResponse.from(service, profissionaisDoServico(profissionais, service));
    }

    private List<Profissional> profissionaisDoServico(List<Profissional> todos, ServiceOffering servico) {
        return todos.stream()
                .filter(p -> p.getServicos().stream().anyMatch(s -> s.getId().equals(servico.getId())))
                .toList();
    }

}
