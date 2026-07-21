package com.castilhodigital.facilitei.common.exception;

/** Lancada quando uma regra de negocio e violada (ex.: slug duplicado, slot indisponivel). Mapeada para HTTP 400/409 na etapa 4. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }

}
