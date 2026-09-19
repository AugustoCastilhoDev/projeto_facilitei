package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendaConfigService;
import com.castilhodigital.facilitei.agenda.BloqueioService;
import com.castilhodigital.facilitei.agenda.BloqueioService.BloqueioCriado;
import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Folgas, ferias e ausencias de um profissional. Bloquear nao cancela agendamentos ja marcados: so avisa quantos sao. */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/profissionais/{profissionalId}/bloqueios")
@RequiredArgsConstructor
public class BloqueioAdminController {

    /** Resposta da criacao: o bloqueio e quantos agendamentos ja marcados caem no periodo (para o dono resolver). */
    public record BloqueioCriadoResponse(BloqueioResponse bloqueio, int agendamentosAfetados) {
    }

    private final BloqueioService bloqueioService;
    private final AgendaConfigService agendaConfigService;
    private final TenantSecurityGuard tenantSecurityGuard;
    private final Clock clock;

    /** So os que ainda nao terminaram (em andamento ou futuros). */
    @GetMapping
    public List<BloqueioResponse> listar(@PathVariable Long tenantId, @PathVariable Long profissionalId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        ZoneId fuso = fuso(tenantId);
        return bloqueioService.listarEmAndamentoOuFuturos(tenantId, profissionalId, Instant.now(clock)).stream()
                .map(bloqueio -> BloqueioResponse.de(bloqueio, fuso))
                .toList();
    }

    @PostMapping
    public ResponseEntity<BloqueioCriadoResponse> criar(@PathVariable Long tenantId, @PathVariable Long profissionalId,
                                                         @Valid @RequestBody BloqueioRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        BloqueioCriado criado = bloqueioService.criar(
                tenantId, profissionalId, request.inicio().toInstant(), request.fim().toInstant(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(new BloqueioCriadoResponse(
                BloqueioResponse.de(criado.bloqueio(), fuso(tenantId)), criado.agendamentosAfetados()));
    }

    @DeleteMapping("/{bloqueioId}")
    public ResponseEntity<Void> excluir(@PathVariable Long tenantId, @PathVariable Long profissionalId,
                                         @PathVariable Long bloqueioId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        bloqueioService.excluir(tenantId, profissionalId, bloqueioId);
        return ResponseEntity.noContent().build();
    }

    private ZoneId fuso(Long tenantId) {
        return ZoneId.of(agendaConfigService.buscar(tenantId).fusoHorario());
    }

}
