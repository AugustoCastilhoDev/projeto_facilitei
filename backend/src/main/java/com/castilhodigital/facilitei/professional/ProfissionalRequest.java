package com.castilhodigital.facilitei.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record ProfissionalRequest(

        @NotBlank(message = "Nome e obrigatorio.")
        String nome,

        @NotNull(message = "Horario de abertura e obrigatorio.")
        LocalTime horarioAbertura,

        @NotNull(message = "Horario de fechamento e obrigatorio.")
        LocalTime horarioFechamento,

        @NotNull(message = "Lista de servicos e obrigatoria (pode ser vazia).")
        List<Long> servicoIds

) {
}
