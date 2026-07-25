package com.castilhodigital.facilitei.common.exception;

/**
 * Falha ao enviar uma notificacao que precisa de confirmacao sincrona (ex.:
 * codigo de verificacao via WhatsApp) - diferente do NotificationService
 * "melhor esforco" usado pelo fluxo de agendamento, aqui o chamador precisa
 * saber que o envio nao aconteceu. Mapeada para HTTP 502.
 */
public class EnvioNotificacaoFalhouException extends RuntimeException {

    public EnvioNotificacaoFalhouException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

}
