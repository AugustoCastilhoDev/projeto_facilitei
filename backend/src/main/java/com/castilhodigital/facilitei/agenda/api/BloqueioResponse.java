package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.BloqueioService.BloqueioView;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record BloqueioResponse(Long id, Long profissionalId, OffsetDateTime inicio, OffsetDateTime fim, String motivo) {

    public static BloqueioResponse de(BloqueioView bloqueio, ZoneId fuso) {
        return new BloqueioResponse(bloqueio.id(), bloqueio.profissionalId(),
                bloqueio.inicio().atZone(fuso).toOffsetDateTime(), bloqueio.fim().atZone(fuso).toOffsetDateTime(),
                bloqueio.motivo());
    }

}
