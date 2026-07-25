package com.castilhodigital.facilitei.auth;

import jakarta.validation.constraints.NotBlank;

public record SolicitarCodigoRequest(

        @NotBlank(message = "Telefone e obrigatorio.")
        String telefone

) {
}
