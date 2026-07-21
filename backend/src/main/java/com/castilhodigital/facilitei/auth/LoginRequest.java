package com.castilhodigital.facilitei.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email e obrigatorio.")
        String email,

        @NotBlank(message = "Senha e obrigatoria.")
        String senha

) {
}
