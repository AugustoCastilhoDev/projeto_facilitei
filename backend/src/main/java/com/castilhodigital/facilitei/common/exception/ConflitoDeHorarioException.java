package com.castilhodigital.facilitei.common.exception;

/**
 * O horario pedido nao esta (mais) livre: outra pessoa reservou antes, o
 * profissional foi bloqueado, ou o horario nunca fez parte da agenda. Mapeada
 * para HTTP 409 - diferente de RegraDeNegocioException (400), aqui o pedido
 * estava bem formado, so perdeu a corrida, e o cliente deve escolher outro horario.
 */
public class ConflitoDeHorarioException extends RuntimeException {

    public ConflitoDeHorarioException(String mensagem) {
        super(mensagem);
    }

}
