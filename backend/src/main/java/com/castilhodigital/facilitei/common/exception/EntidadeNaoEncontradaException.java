package com.castilhodigital.facilitei.common.exception;

/** Lancada quando um recurso buscado por id/slug/etc. nao existe. Mapeada para HTTP 404 na etapa 4. */
public class EntidadeNaoEncontradaException extends RuntimeException {

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

}
