package com.castilhodigital.facilitei.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementacao provisoria: escreve a notificacao no log em vez de enviar WhatsApp de verdade. */
@Slf4j
@Service
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void enviar(String telefone, String mensagem) {
        log.info("[NOTIFICACAO -> {}] {}", telefone, mensagem);
    }

}
