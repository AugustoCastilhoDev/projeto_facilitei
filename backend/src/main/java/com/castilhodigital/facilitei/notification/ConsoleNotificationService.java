package com.castilhodigital.facilitei.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacao provisoria: escreve a notificacao no log em vez de enviar
 * WhatsApp de verdade. E o default (facilitei.notification.provider nao
 * definido, ou definido como "console") - trocar para "myzap" ativa
 * MyZapNotificationService no lugar desta.
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "facilitei.notification", name = "provider", havingValue = "console", matchIfMissing = true)
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void enviar(String telefone, String mensagem) {
        log.info("[NOTIFICACAO -> {}] {}", telefone, mensagem);
    }

}
