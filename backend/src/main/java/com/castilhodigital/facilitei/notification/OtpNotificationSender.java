package com.castilhodigital.facilitei.notification;

/**
 * Porta de envio de codigo de verificacao (OTP) ao cliente final via
 * WhatsApp. Deliberadamente separada de NotificationService: aquela e
 * "melhor esforco" (nunca pode derrubar a transacao do agendamento), esta
 * precisa do contrario - o chamador precisa saber de forma sincrona se o
 * envio funcionou, entao a implementacao deve deixar a falha propagar (ver
 * common.exception.EnvioNotificacaoFalhouException).
 */
public interface OtpNotificationSender {

    void enviarCodigo(String telefone, String codigo);

}
