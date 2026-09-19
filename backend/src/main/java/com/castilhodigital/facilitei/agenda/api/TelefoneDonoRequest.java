package com.castilhodigital.facilitei.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelefoneDonoRequest(

        @NotBlank(message = "Informe o WhatsApp do dono.")
        @Size(max = 30, message = "Telefone longo demais.")
        String telefone

) {
}
