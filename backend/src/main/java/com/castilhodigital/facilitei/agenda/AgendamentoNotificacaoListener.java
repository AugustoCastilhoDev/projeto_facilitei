package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa por WhatsApp quando o agendamento muda de estado - o cliente na maioria dos casos, o dono quando ha um
 * Pix para conferir - so depois do commit, entao nunca ha mensagem de algo que sofreu rollback. Falha no envio
 * e engolida: notificar e um efeito colateral, nao pode desfazer nem travar a reserva (o commit ja aconteceu
 * quando este ouvinte roda).
 */
@Slf4j
@Component
class AgendamentoNotificacaoListener {

    private final NotificationService notificationService;
    private final AvisoAoDono avisoAoDono;
    private final String urlBaseDoFrontend;

    AgendamentoNotificacaoListener(NotificationService notificationService, AvisoAoDono avisoAoDono,
                                    @Value("${facilitei.frontend.base-url}") String urlBaseDoFrontend) {
        this.notificationService = notificationService;
        this.avisoAoDono = avisoAoDono;
        this.urlBaseDoFrontend = urlBaseDoFrontend.replaceAll("/+$", "");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void aoMudarDeEstado(AgendamentoEvento evento) {
        try {
            if (evento.tipo().paraODono()) {
                avisoAoDono.avisar(evento.telefoneDono(), mensagemParaODono(evento),
                        "agendamento " + evento.agendamentoId() + ", " + evento.tipo());
            } else {
                notificationService.enviar(evento.clienteTelefone(), mensagem(evento));
            }
        } catch (RuntimeException ex) {
            log.warn("Falha ao notificar sobre o agendamento {} ({}): {}",
                    evento.agendamentoId(), evento.tipo(), ex.getMessage());
        }
    }

    /** Texto para o CLIENTE; os eventos que o dono recebe vao por mensagemParaODono. */
    static String mensagem(AgendamentoEvento evento) {
        String quando = TextosDeAviso.quando(evento.inicio(), evento.fuso());
        return switch (evento.tipo()) {
            case CONFIRMADO -> "Olá, %s! Seu horário na %s está confirmado: %s."
                    .formatted(evento.clienteNome(), evento.nomeNegocio(), quando);
            case AGUARDANDO_PAGAMENTO -> pedidoDePagamento(evento, quando);
            case EXPIRADO -> evento.pagamentoInformado()
                    ? "O estabelecimento não confirmou o seu pagamento do sinal a tempo e o horário na %s (%s) foi liberado. Se você pagou, entre em contato com o estabelecimento."
                            .formatted(evento.nomeNegocio(), quando)
                    : "O prazo para pagar o sinal do seu horário na %s (%s) acabou e o horário foi liberado. Se ainda quiser, faça uma nova reserva."
                            .formatted(evento.nomeNegocio(), quando);
            case CANCELADO_PELO_NEGOCIO -> "Seu horário na %s (%s) foi cancelado pelo estabelecimento."
                    .formatted(evento.nomeNegocio(), quando);
            case PAGAMENTO_NAO_LOCALIZADO -> "A %s não encontrou o pagamento do sinal do seu horário (%s), então o horário foi liberado. Se você pagou, entre em contato com o estabelecimento."
                    .formatted(evento.nomeNegocio(), quando);
            case PAGAMENTO_INFORMADO -> throw new IllegalArgumentException(
                    "PAGAMENTO_INFORMADO e mensagem para o dono (ver mensagemParaODono).");
        };
    }

    private static String pedidoDePagamento(AgendamentoEvento evento, String quando) {
        String ate = TextosDeAviso.hora(evento.expiraEm(), evento.fuso());
        if (evento.meioCobranca() == MeioCobranca.PIX_PROPRIO) {
            return "Olá, %s! Reservamos seu horário na %s (%s). Pague o sinal de %s via Pix até às %s e toque em \"Já paguei\" na tela de pagamento para o estabelecimento confirmar."
                    .formatted(evento.clienteNome(), evento.nomeNegocio(), quando,
                            TextosDeAviso.dinheiro(evento.valorSinal()), ate);
        }
        return "Olá, %s! Reservamos seu horário na %s (%s). Pague o sinal via Pix até às %s para confirmar."
                .formatted(evento.clienteNome(), evento.nomeNegocio(), quando, ate);
    }

    /** Texto para o DONO: o pedido de conferir um Pix, com o link secreto que leva a tela de confirmar/recusar. */
    String mensagemParaODono(AgendamentoEvento evento) {
        if (evento.tipo() != AgendamentoEvento.Tipo.PAGAMENTO_INFORMADO) {
            throw new IllegalArgumentException("Sem mensagem para o dono para " + evento.tipo());
        }
        return TextosDeAviso.pedidoDeConfirmacao(evento, urlBaseDoFrontend + "/confirmar-sinal/" + evento.tokenConfirmacao());
    }

}
