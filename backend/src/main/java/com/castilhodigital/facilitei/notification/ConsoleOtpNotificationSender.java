package com.castilhodigital.facilitei.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacao provisoria: escreve o codigo no log em vez de enviar
 * WhatsApp de verdade - mesmo criterio de ativacao que ConsoleNotificationService
 * (default quando facilitei.notification.provider nao e "myzap").
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "facilitei.notification", name = "provider", havingValue = "console", matchIfMissing = true)
public class ConsoleOtpNotificationSender implements OtpNotificationSender {

    @Override
    public void enviarCodigo(String telefone, String codigo) {
        log.info("[CODIGO DE VERIFICACAO -> {}] {}", telefone, codigo);
    }

}
