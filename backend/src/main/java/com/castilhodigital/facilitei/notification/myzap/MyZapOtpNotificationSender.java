package com.castilhodigital.facilitei.notification.myzap;

import com.castilhodigital.facilitei.common.exception.EnvioNotificacaoFalhouException;
import com.castilhodigital.facilitei.notification.NotificationGatewayException;
import com.castilhodigital.facilitei.notification.OtpNotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Envia o codigo de verificacao via MyZap. Vive neste pacote (em vez de
 * "notification" puro) especificamente para reusar o MyZapClient
 * package-private sem abrir sua visibilidade.
 *
 * Ao contrario de MyZapNotificationService (melhor esforco, engole falha),
 * aqui a falha e deixada propagar como EnvioNotificacaoFalhouException: o
 * cliente do portal precisa saber, na hora, se o codigo nao foi enviado -
 * nao ha booking em andamento para proteger de rollback.
 */
@Service
@ConditionalOnProperty(prefix = "facilitei.notification", name = "provider", havingValue = "myzap")
@RequiredArgsConstructor
public class MyZapOtpNotificationSender implements OtpNotificationSender {

    private final MyZapClient myZapClient;

    @Override
    public void enviarCodigo(String telefone, String codigo) {
        String mensagem = "Seu codigo de verificacao Facilitei e " + codigo + ". Valido por 5 minutos.";
        try {
            myZapClient.enviarTexto(normalizar(telefone), mensagem);
        } catch (NotificationGatewayException ex) {
            throw new EnvioNotificacaoFalhouException(
                    "Falha ao enviar codigo de verificacao via MyZap para " + telefone, ex);
        }
    }

    /** A API do MyZap espera so digitos (ex.: "5563999999999"), sem "+" nem formatacao. */
    private String normalizar(String telefone) {
        return telefone.replaceAll("[^0-9]", "");
    }

}
