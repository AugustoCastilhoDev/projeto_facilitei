package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.exception.ConflitoDeHorarioException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Trata os avisos de pagamento do gateway (webhook da Asaas) para agendamentos.
 * Sem @Transactional: cada passo e uma transacao propria em AgendamentoService,
 * porque a reativacao tardia pode ser recusada pelo banco e o que fazer depois
 * (alertar o operador) nao pode depender dessa transacao.
 *
 * O caso dificil e o pagamento que chega DEPOIS da expiracao: o cliente pagou, mas o
 * horario pode ja ser de outra pessoa. Quando e possivel, o agendamento volta; quando
 * nao, o dinheiro entrou por algo que nao existe - ninguem pode ignorar isso, entao
 * vira um alerta de erro (log + Sentry) para o dono devolver o sinal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoPagamentoService {

    private final AgendamentoService agendamentoService;

    /** De qual negocio e a cobranca, se ela pertence a um agendamento da nova agenda (para validar o token do webhook). */
    public Optional<Tenant> buscarTenantDaCobranca(String cobrancaExternaId) {
        return agendamentoService.buscarTenantDaCobranca(cobrancaExternaId);
    }

    public void processarPagamentoRecebido(String cobrancaExternaId, Instant agora) {
        switch (agendamentoService.registrarPagamento(cobrancaExternaId, agora)) {
            case CONFIRMADO -> log.info("Sinal pago e confirmado (cobranca {}).", cobrancaExternaId);
            case HORARIO_LIBERADO -> tentarReativar(cobrancaExternaId, agora);
            case CANCELADO -> alertar("Sinal recebido para um agendamento CANCELADO - devolver o valor ao cliente", cobrancaExternaId);
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
                    cobrancaExternaId);
        }
    }

    private void alertar(String mensagem, String cobrancaExternaId) {
        String texto = mensagem + " [cobranca " + cobrancaExternaId + "]";
        log.error(texto);
        Sentry.captureMessage(texto, SentryLevel.ERROR);
    }

}
