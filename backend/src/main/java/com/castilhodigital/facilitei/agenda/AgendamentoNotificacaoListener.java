package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.notification.NotificationService;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa o cliente por WhatsApp quando o agendamento muda de estado - so depois
 * do commit, entao nunca ha mensagem de algo que sofreu rollback. Falha no
 * envio e engolida: notificar e um efeito colateral, nao pode desfazer nem
 * travar a reserva (o commit ja aconteceu quando este ouvinte roda).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AgendamentoNotificacaoListener {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void aoMudarDeEstado(AgendamentoEvento evento) {
        try {
            notificationService.enviar(evento.clienteTelefone(), mensagem(evento));
        } catch (RuntimeException ex) {
            log.warn("Falha ao notificar o cliente do agendamento {} ({}): {}",
                    evento.agendamentoId(), evento.tipo(), ex.getMessage());
        }
    }

    static String mensagem(AgendamentoEvento evento) {
        String quando = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR)
                .withZone(evento.fuso()).format(evento.inicio());
        return switch (evento.tipo()) {
            case CONFIRMADO -> "Olá, %s! Seu horário na %s está confirmado: %s."
                    .formatted(evento.clienteNome(), evento.nomeNegocio(), quando);
            case AGUARDANDO_PAGAMENTO -> "Olá, %s! Reservamos seu horário na %s (%s). Pague o sinal via Pix até às %s para confirmar."
                    .formatted(evento.clienteNome(), evento.nomeNegocio(), quando,
                            DateTimeFormatter.ofPattern("HH:mm").withZone(evento.fuso()).format(evento.expiraEm()));
            case EXPIRADO -> "O prazo para pagar o sinal do seu horário na %s (%s) acabou e o horário foi liberado. Se ainda quiser, faça uma nova reserva."
                    .formatted(evento.nomeNegocio(), quando);
            case CANCELADO_PELO_NEGOCIO -> "Seu horário na %s (%s) foi cancelado pelo estabelecimento."
                    .formatted(evento.nomeNegocio(), quando);
        };
    }

}
