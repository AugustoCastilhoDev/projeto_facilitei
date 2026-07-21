package com.castilhodigital.facilitei.common.exception;

/** Usuario autenticado tentando acessar dados de um tenant que nao e o seu. Mapeada para HTTP 403. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

}
