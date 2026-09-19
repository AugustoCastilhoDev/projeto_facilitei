package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.TextosDeAviso.SemHorario;
import com.castilhodigital.facilitei.common.exception.ConflitoDeHorarioException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Trata os avisos de pagamento do sinal: o webhook do gateway (Asaas) e o "ja paguei" do cliente (Pix proprio).
 * Sem @Transactional: cada passo e uma transacao propria em AgendamentoService, porque a reativacao tardia pode
 * ser recusada pelo banco e o que fazer depois (alertar o dono) nao pode depender dessa transacao.
 *
 * O caso dificil e o pagamento que chega DEPOIS da expiracao: o cliente pagou, mas o horario pode ja ser de outra
 * pessoa. Quando e possivel, o agendamento volta; quando nao, o dinheiro entrou por algo que nao existe - ninguem
 * pode ignorar isso, entao vira um alerta de erro (log + Sentry) E uma mensagem no WhatsApp do dono, que e quem
 * devolve o sinal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoPagamentoService {

    private final AgendamentoService agendamentoService;
    private final AvisoAoDono avisoAoDono;

    /** De qual negocio e a cobranca, se ela pertence a um agendamento da nova agenda (para validar o token do webhook). */
    public Optional<Tenant> buscarTenantDaCobranca(String cobrancaExternaId) {
        return agendamentoService.buscarTenantDaCobranca(cobrancaExternaId);
    }

    public void processarPagamentoRecebido(String cobrancaExternaId, Instant agora) {
        switch (agendamentoService.registrarPagamento(cobrancaExternaId, agora)) {
            case CONFIRMADO -> log.info("Sinal pago e confirmado (cobranca {}).", cobrancaExternaId);
            case HORARIO_LIBERADO -> tentarReativar(cobrancaExternaId, agora);
            case CANCELADO -> alertar("Sinal recebido para um agendamento CANCELADO - devolver o valor ao cliente",
                    cobrancaExternaId, SemHorario.AGENDAMENTO_CANCELADO);
            case JA_ESTAVA_CONFIRMADO, DESCONHECIDO -> log.debug("Pagamento {} sem nada a fazer.", cobrancaExternaId);
        }
    }

    private void tentarReativar(String cobrancaExternaId, Instant agora) {
        try {
            agendamentoService.reativarPorPagamentoTardio(cobrancaExternaId, agora);
            log.info("Sinal pago apos a expiracao e o horario ainda estava livre: agendamento reativado (cobranca {}).",
                    cobrancaExternaId);
        } catch (ConflitoDeHorarioException | RegraDeNegocioException ex) {
            alertar("Sinal recebido depois da expiracao e o horario ja foi ocupado por outra pessoa - devolver o valor ao cliente",
                    cobrancaExternaId, SemHorario.HORARIO_OCUPADO_POR_OUTRA_PESSOA);
        }
    }

    /**
     * O cliente tocou em "Ja paguei" (Pix proprio). Se a reserva ainda estava de pe, o dono e chamado a conferir (isso
     * acontece no evento, depois do commit). Se ela ja tinha expirado e outra pessoa ficou com o horario, o cliente
     * recebe o 409 e o dono e avisado por aqui: o Pix pode ter caido na conta dele por um horario que nao existe mais.
     */
    public AgendamentoView informarPagamentoDoCliente(String slug, UUID codigoPublico, Instant agora) {
        try {
            return agendamentoService.informarPagamento(slug, codigoPublico, agora);
        } catch (ConflitoDeHorarioException ex) {
            AgendamentoView agendamento = agendamentoService.buscarPorCodigoPublico(slug, codigoPublico);
            String texto = TextosDeAviso.pagamentoSemHorario(agendamento, SemHorario.HORARIO_OCUPADO_POR_OUTRA_PESSOA, true);
            log.warn("Cliente avisou pagamento de uma reserva expirada cujo horario ja foi ocupado (agendamento {}).",
                    agendamento.id());
            avisoAoDono.avisar(agendamento.telefoneDono(), texto, "agendamento " + agendamento.id() + ", aviso apos o prazo");
            throw ex;
        }
    }

    private void alertar(String mensagem, String cobrancaExternaId, SemHorario situacao) {
        String texto = mensagem + " [cobranca " + cobrancaExternaId + "]";
        log.error(texto);
        Sentry.captureMessage(texto, SentryLevel.ERROR);
        agendamentoService.buscarPorCobrancaExterna(cobrancaExternaId).ifPresent(agendamento -> avisoAoDono.avisar(
                agendamento.telefoneDono(), TextosDeAviso.pagamentoSemHorario(agendamento, situacao, false),
                "agendamento " + agendamento.id() + ", cobranca " + cobrancaExternaId));
    }

}
