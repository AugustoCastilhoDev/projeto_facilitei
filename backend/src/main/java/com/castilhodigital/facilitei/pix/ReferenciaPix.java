package com.castilhodigital.facilitei.pix;

/**
 * O identificador que acompanha o Pix de um agendamento (o txid do BR Code): o dono o ve no comprovante do
 * banco e a mensagem de confirmacao o cita, entao ele sabe qual agendamento aquele Pix quita.
 */
public final class ReferenciaPix {

    private static final String PREFIXO = "FAC";

    private ReferenciaPix() {
    }

    /** "FAC42" para o agendamento 42: so letras e numeros, como o txid exige. */
    public static String doAgendamento(Long agendamentoId) {
        return PREFIXO + agendamentoId;
    }

}
