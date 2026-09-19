package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoCheckoutService;
import com.castilhodigital.facilitei.agenda.AgendamentoOrigem;
import com.castilhodigital.facilitei.agenda.AgendamentoPagamentoService;
import com.castilhodigital.facilitei.agenda.CheckoutAgendamento;
import com.castilhodigital.facilitei.agenda.DisponibilidadeService;
import com.castilhodigital.facilitei.agenda.DisponibilidadeService.ProfissionalResumo;
import com.castilhodigital.facilitei.agenda.NovoAgendamento;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
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
 * API publica (sem login) do agendamento pelo link do negocio: escolher servicos,
 * profissional e horario, e reservar. O negocio vem do slug; o tenantId nunca e informado pelo cliente.
 *
 * Fluxo: GET profissionais (quem faz esses servicos) -> GET disponibilidade (horarios livres do dia)
 * -> POST agendamentos (reserva; com sinal devolve o Pix) -> GET agendamentos/{codigo} (polling ate confirmar).
 */
@RestController
@RequestMapping("/api/public/tenants/{slug}/agenda")
@RequiredArgsConstructor
public class PublicAgendaController {

    private final TenantService tenantService;
    private final DisponibilidadeService disponibilidadeService;
    private final AgendamentoCheckoutService checkoutService;
    private final AgendamentoPagamentoService pagamentoService;
    private final Clock clock;

    @GetMapping("/profissionais")
    public List<ProfissionalResumo> listarProfissionais(@PathVariable String slug, @RequestParam List<Long> servicoIds) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        return disponibilidadeService.listarProfissionaisHabilitados(tenant.getId(), servicoIds);
    }

    @GetMapping("/disponibilidade")
    public List<HorarioDisponivelResponse> listarDisponibilidade(
            @PathVariable String slug,
            @RequestParam List<Long> servicoIds,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        ZoneId fuso = ZoneId.of(tenant.getFusoHorario());
        return disponibilidadeService
                .listar(tenant.getId(), servicoIds, profissionalId, data, Instant.now(clock)).stream()
                .map(horario -> HorarioDisponivelResponse.de(horario, fuso))
                .toList();
    }

    @PostMapping("/agendamentos")
    public ResponseEntity<AgendamentoPublicoResponse> criar(@PathVariable String slug,
                                                             @Valid @RequestBody CriarAgendamentoRequest request) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        CheckoutAgendamento checkout = checkoutService.iniciar(new NovoAgendamento(
                tenant.getId(), request.servicoIds(), request.profissionalId(), request.inicio().toInstant(),
                request.clienteNome().trim(), request.clienteTelefone(), request.clienteCpfCnpj(),
                null, AgendamentoOrigem.PUBLICO), Instant.now(clock));
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoPublicoResponse.de(checkout));
    }

    /** Polling do status do pagamento; enquanto pendente, devolve tambem o Pix (a tela sobrevive a um F5). */
    @GetMapping("/agendamentos/{codigo}")
    public AgendamentoPublicoResponse buscar(@PathVariable String slug, @PathVariable UUID codigo) {
        return AgendamentoPublicoResponse.de(checkoutService.consultar(slug, codigo));
    }

    /**
     * "Ja paguei" (so Pix na chave do dono): o horario passa a ficar segurado por mais tempo e o dono recebe o pedido
     * de conferir. Repetir a chamada nao gera outro pedido. 409 se a reserva ja expirou e o horario foi ocupado.
     */
    @PostMapping("/agendamentos/{codigo}/informar-pagamento")
    public AgendamentoPublicoResponse informarPagamento(@PathVariable String slug, @PathVariable UUID codigo) {
        return AgendamentoPublicoResponse.de(new CheckoutAgendamento(
                pagamentoService.informarPagamentoDoCliente(slug, codigo, Instant.now(clock)), null));
    }

}
