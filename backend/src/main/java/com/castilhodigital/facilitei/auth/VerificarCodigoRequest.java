package com.castilhodigital.facilitei.auth;

import jakarta.validation.constraints.NotBlank;

public record VerificarCodigoRequest(

        @NotBlank(message = "Telefone e obrigatorio.")
        String telefone,

        @NotBlank(message = "Codigo e obrigatorio.")
        String codigo,

        /** So e usado se ainda nao existir um Cliente para este telefone (primeiro login = cadastro). */
        String nome

) {
}
