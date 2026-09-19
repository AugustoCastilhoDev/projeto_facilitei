package com.castilhodigital.facilitei.agenda.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Periodo em que o profissional nao atende (folga, ferias, consulta medica...). */
public record BloqueioRequest(

        @NotNull(message = "Informe o início.")
        OffsetDateTime inicio,

        @NotNull(message = "Informe o fim.")
        OffsetDateTime fim,

        @Size(max = 150, message = "O motivo deve ter no máximo 150 caracteres.")
        String motivo

) {
}
