package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manda uma mensagem ao WhatsApp do dono do negocio. Melhor esforco: quem chama ja gravou o que importa no
 * banco e no log, e um WhatsApp fora do ar (ou um dono sem numero cadastrado) nao pode desfazer nem travar nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AvisoAoDono {

    private final NotificationService notificationService;

    /** @param contexto so para o log ("agendamento 42"): a mensagem em si pode ter dado pessoal e nao vai para o log */
    void avisar(String telefoneDono, String mensagem, String contexto) {
        if (telefoneDono == null || telefoneDono.isBlank()) {
            log.warn("Nao foi possivel avisar o dono ({}): o negocio nao tem WhatsApp do dono cadastrado.", contexto);
            return;
        }
        try {
            notificationService.enviar(telefoneDono, mensagem);
        } catch (RuntimeException ex) {
            log.warn("Falha ao avisar o dono ({}): {}", contexto, ex.getMessage());
        }
    }

}
