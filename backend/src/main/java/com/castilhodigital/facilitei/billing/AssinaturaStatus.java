package com.castilhodigital.facilitei.billing;

/**
 * Status da assinatura do tenant com a propria plataforma (nao confundir com
 * PaymentStatus, que e do sinal Pix do cliente final - BYOPP).
 */
public enum AssinaturaStatus {

    /** Dentro do periodo de trial gratuito - uso liberado. */
    TRIAL,

    /** Mensalidade em dia - uso liberado. */
    ATIVA,

    /** Trial encerrado ou mensalidade vencida, com fatura pendente - bloqueia acoes de novo uso (ver AssinaturaGuard). */
    INADIMPLENTE,

    /** Assinatura cancelada pelo proprio tenant - bloqueia acoes de novo uso. */
    CANCELADA

}
