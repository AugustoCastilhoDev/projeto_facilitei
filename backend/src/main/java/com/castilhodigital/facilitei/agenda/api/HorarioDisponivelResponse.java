package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.DisponibilidadeService.HorarioDisponivel;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Um horario oferecido ao cliente, com o deslocamento do fuso do negocio (2026-09-21T10:00:00-03:00). */
public record HorarioDisponivelResponse(OffsetDateTime inicio, OffsetDateTime fim, Long profissionalId, String profissionalNome) {

    public static HorarioDisponivelResponse de(HorarioDisponivel horario, ZoneId fuso) {
        return new HorarioDisponivelResponse(
                horario.inicio().atZone(fuso).toOffsetDateTime(),
                horario.fim().atZone(fuso).toOffsetDateTime(),
                horario.profissionalId(),
                horario.profissionalNome());
    }

}
