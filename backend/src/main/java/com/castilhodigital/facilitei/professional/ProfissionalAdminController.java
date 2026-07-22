package com.castilhodigital.facilitei.professional;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
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
 * CRUD de profissionais do painel admin - mesmo formato de
 * ServiceOfferingAdminController (TenantSecurityGuard confere o tenantId do
 * path contra o do JWT autenticado em toda requisicao).
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/profissionais")
@RequiredArgsConstructor
public class ProfissionalAdminController {

    private final ProfissionalService profissionalService;
    private final TenantService tenantService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public List<ProfissionalResponse> listar(@PathVariable Long tenantId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return profissionalService.listarTodos(tenantId).stream()
                .map(ProfissionalResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProfissionalResponse> criar(@PathVariable Long tenantId,
                                                       @Valid @RequestBody ProfissionalRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Tenant tenant = tenantService.buscarPorId(tenantId);
        Profissional profissional = profissionalService.criar(
                tenant, request.nome(), request.horarioAbertura(), request.horarioFechamento(), request.servicoIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfissionalResponse.from(profissional));
    }

    @PutMapping("/{profissionalId}")
    public ProfissionalResponse atualizar(@PathVariable Long tenantId, @PathVariable Long profissionalId,
                                           @Valid @RequestBody ProfissionalRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        Profissional profissional = profissionalService.atualizar(tenantId, profissionalId,
                request.nome(), request.horarioAbertura(), request.horarioFechamento(), request.servicoIds());
        return ProfissionalResponse.from(profissional);
    }

    @DeleteMapping("/{profissionalId}")
    public ResponseEntity<Void> desativar(@PathVariable Long tenantId, @PathVariable Long profissionalId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        profissionalService.desativar(tenantId, profissionalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{profissionalId}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long tenantId, @PathVariable Long profissionalId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        profissionalService.ativar(tenantId, profissionalId);
        return ResponseEntity.noContent().build();
    }

}
