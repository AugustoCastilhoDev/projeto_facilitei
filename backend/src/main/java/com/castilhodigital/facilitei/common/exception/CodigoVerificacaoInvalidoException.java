package com.castilhodigital.facilitei.common.exception;

/** Codigo de verificacao (OTP) incorreto, expirado ou ja usado. Mapeada para HTTP 401. */
public class CodigoVerificacaoInvalidoException extends RuntimeException {

    public CodigoVerificacaoInvalidoException() {
        super("Codigo invalido ou expirado.");
    }

}
