package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoOrigem;
import com.castilhodigital.facilitei.agenda.AgendamentoService;
import com.castilhodigital.facilitei.agenda.NovoAgendamento;
import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agenda do painel: ver os agendamentos de um periodo, criar um (cliente do balcao/telefone) e conduzir o
 * ciclo de vida (confirmar sinal, cancelar, concluir, marcar falta). Como todo endpoint admin, o
 * TenantSecurityGuard confere o tenantId do path contra o do JWT.
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/agendamentos")
@RequiredArgsConstructor
public class AgendamentoAdminController {

    /** Limite da janela de consulta: uma tela de agenda mostra dia, semana ou mes, nunca "tudo". */
    private static final long MAX_DIAS_DA_JANELA = 62;

    private final AgendamentoService agendamentoService;
    private final TenantSecurityGuard tenantSecurityGuard;
    private final Clock clock;

    /** {@code ate} e opcional (padrao: so o dia de {@code de}); ambos inclusivos, no fuso do negocio. */
    @GetMapping
    public List<AgendamentoAdminResponse> listar(
            @PathVariable Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long profissionalId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        LocalDate fim = ate == null ? de : ate;
        if (fim.isBefore(de)) {
            throw new RegraDeNegocioException("A data final não pode ser anterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(de, fim) > MAX_DIAS_DA_JANELA) {
            throw new RegraDeNegocioException("Consulte no máximo " + MAX_DIAS_DA_JANELA + " dias por vez.");
        }
        return agendamentoService.listarAgenda(tenantId, profissionalId, de, fim).stream()
                .map(AgendamentoAdminResponse::de)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AgendamentoAdminResponse> criar(@PathVariable Long tenantId,
                                                           @Valid @RequestBody CriarAgendamentoPainelRequest request) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        var criado = agendamentoService.criarPeloPainel(new NovoAgendamento(
                tenantId, request.servicoIds(), request.profissionalId(), request.inicio().toInstant(),
                request.clienteNome().trim(), request.clienteTelefone(), null, null, AgendamentoOrigem.PAINEL),
                Instant.now(clock));
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoAdminResponse.de(criado));
    }

    /** O dono confirma que recebeu o sinal (Pix com confirmacao manual, ou pagamento que o gateway nao avisou). */
    @PostMapping("/{agendamentoId}/confirmar-pagamento")
    public AgendamentoAdminResponse confirmarPagamento(@PathVariable Long tenantId, @PathVariable Long agendamentoId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendamentoAdminResponse.de(agendamentoService.confirmarSinalManual(tenantId, agendamentoId, Instant.now(clock)));
    }

    /** O dono conferiu o extrato e nao achou o Pix: recusa, o horario volta para a agenda e o cliente e avisado. */
    @PostMapping("/{agendamentoId}/recusar-pagamento")
    public AgendamentoAdminResponse recusarPagamento(@PathVariable Long tenantId, @PathVariable Long agendamentoId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendamentoAdminResponse.de(agendamentoService.recusarPagamentoManual(tenantId, agendamentoId));
    }

    @PostMapping("/{agendamentoId}/cancelar")
    public AgendamentoAdminResponse cancelar(@PathVariable Long tenantId, @PathVariable Long agendamentoId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendamentoAdminResponse.de(agendamentoService.cancelarPeloNegocio(tenantId, agendamentoId));
    }

    @PostMapping("/{agendamentoId}/concluir")
    public AgendamentoAdminResponse concluir(@PathVariable Long tenantId, @PathVariable Long agendamentoId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendamentoAdminResponse.de(agendamentoService.concluir(tenantId, agendamentoId, Instant.now(clock)));
    }

    @PostMapping("/{agendamentoId}/faltou")
    public AgendamentoAdminResponse marcarFalta(@PathVariable Long tenantId, @PathVariable Long agendamentoId) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        return AgendamentoAdminResponse.de(agendamentoService.marcarFalta(tenantId, agendamentoId, Instant.now(clock)));
    }

}
