package com.castilhodigital.facilitei.notification.myzap;

import com.castilhodigital.facilitei.notification.NotificationGatewayException;
import com.castilhodigital.facilitei.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Envia a notificacao de verdade via WhatsApp usando o MyZap. Ativado com
 * facilitei.notification.provider=myzap (default e "console", ver
 * ConsoleNotificationService).
 *
 * Uma falha aqui NUNCA pode derrubar o fluxo de agendamento: quem chama
 * NotificationService.enviar() (BookingService) esta sempre dentro de um
 * @Transactional que tambem grava o booking/slot - se a excecao subisse, uma
 * instabilidade momentanea no WhatsApp faria a reserva inteira dar rollback.
 * Por isso a falha e so logada (best effort), nunca relancada.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "facilitei.notification", name = "provider", havingValue = "myzap")
@RequiredArgsConstructor
public class MyZapNotificationService implements NotificationService {

    private final MyZapClient myZapClient;

    @Override
    public void enviar(String telefone, String mensagem) {
        try {
            myZapClient.enviarTexto(normalizar(telefone), mensagem);
        } catch (NotificationGatewayException ex) {
            log.error("Falha ao enviar notificacao via MyZap para {}", telefone, ex);
        }
    }

    /** A API do MyZap espera so digitos (ex.: "5563999999999"), sem "+" nem formatacao. */
    private String normalizar(String telefone) {
        return telefone.replaceAll("[^0-9]", "");
    }

}
