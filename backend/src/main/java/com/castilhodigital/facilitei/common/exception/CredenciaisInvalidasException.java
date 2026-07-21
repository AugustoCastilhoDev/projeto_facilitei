package com.castilhodigital.facilitei.common.exception;

/** Email/senha invalidos no login. Mapeada para HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Email ou senha invalidos.");
    }

}
