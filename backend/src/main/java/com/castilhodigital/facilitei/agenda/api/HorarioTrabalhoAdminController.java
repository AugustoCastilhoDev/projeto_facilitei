package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.HorarioTrabalhoService;
import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expediente semanal de um profissional. O PUT substitui a semana inteira (a tela salva tudo de uma vez). */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/profissionais/{profissionalId}/horarios")
@RequiredArgsConstructor
public class HorarioTrabalhoAdminController {

    private final HorarioTrabalhoService horarioTrabalhoService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public List<HorarioTrabalhoDto> listar(@PathVariable Long tenantId, @PathVariable Long profissionalId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return horarioTrabalhoService.listar(tenantId, profissionalId).stream().map(HorarioTrabalhoDto::de).toList();
    }

    @PutMapping
    public List<HorarioTrabalhoDto> substituir(
            @PathVariable Long tenantId, @PathVariable Long profissionalId,
            @RequestBody @Size(max = 70, message = "Horários demais para uma semana.") List<@Valid HorarioTrabalhoDto> horarios) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return horarioTrabalhoService
                .substituir(tenantId, profissionalId, horarios.stream().map(HorarioTrabalhoDto::paraIntervalo).toList())
                .stream().map(HorarioTrabalhoDto::de).toList();
    }

}
