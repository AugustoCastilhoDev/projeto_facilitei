package com.castilhodigital.facilitei.common.exception;

/** Excesso de tentativas em uma janela de tempo (ex.: forca bruta no login). Mapeada para HTTP 429. */
public class LimiteDeRequisicoesExcedidoException extends RuntimeException {

    public LimiteDeRequisicoesExcedidoException(String message) {
        super(message);
    }

}
