package com.castilhodigital.facilitei.notification;

/**
 * Porta de notificacao ao cliente final. Os services de dominio (ex.:
 * BookingService) montam a mensagem e chamam enviar() sem saber qual e o
 * canal real - hoje um mock de console, no futuro WhatsApp (Business API
 * ou um provedor tipo Twilio/Z-API), trocavel sem alterar o dominio.
 */
public interface NotificationService {

    void enviar(String telefone, String mensagem);

}
