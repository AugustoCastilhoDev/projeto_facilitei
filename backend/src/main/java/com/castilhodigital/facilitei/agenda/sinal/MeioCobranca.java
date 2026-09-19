package com.castilhodigital.facilitei.agenda.sinal;

/**
 * Por qual meio o sinal de um agendamento e cobrado. Fica gravado no agendamento: o dono pode trocar de
 * meio depois, e uma reserva em andamento precisa seguir as regras do meio com que nasceu.
 */
public enum MeioCobranca {

    /** Cobranca dinamica na conta Asaas do negocio: o gateway avisa (webhook) quando o Pix e pago. */
    ASAAS(false),

    /** Pix "copia e cola" com a chave do proprio dono: nada avisa o pagamento, o dono confere no banco e confirma. */
    PIX_PROPRIO(true);

    private final boolean confirmacaoManual;

    MeioCobranca(boolean confirmacaoManual) {
        this.confirmacaoManual = confirmacaoManual;
    }

    /** O pagamento so passa a valer quando o dono confirma (nao ha aviso automatico do banco). */
    public boolean confirmacaoManual() {
        return confirmacaoManual;
    }

}
