package com.castilhodigital.facilitei.notification;

/** Falha na comunicacao com o provedor de notificacao externo (ex.: MyZap). */
public class NotificationGatewayException extends RuntimeException {

    public NotificationGatewayException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

}
