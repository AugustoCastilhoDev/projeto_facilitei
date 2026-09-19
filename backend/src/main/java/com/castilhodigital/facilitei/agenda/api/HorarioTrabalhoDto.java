package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.HorarioTrabalhoService.Intervalo;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Um intervalo do expediente semanal; serve de pedido e de resposta ("09:00:00" a "12:00:00" na segunda). */
public record HorarioTrabalhoDto(

        @NotNull(message = "Informe o dia da semana.")
        DayOfWeek diaSemana,

        @NotNull(message = "Informe o início.")
        LocalTime inicio,

        @NotNull(message = "Informe o fim.")
        LocalTime fim

) {

    public static HorarioTrabalhoDto de(Intervalo intervalo) {
        return new HorarioTrabalhoDto(intervalo.diaSemana(), intervalo.inicio(), intervalo.fim());
    }

    public Intervalo paraIntervalo() {
        return new Intervalo(diaSemana, inicio, fim);
    }

}
