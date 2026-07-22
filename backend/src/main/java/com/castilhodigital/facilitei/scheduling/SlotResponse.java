package com.castilhodigital.facilitei.scheduling;

import java.time.OffsetDateTime;

public record SlotResponse(
        Long id,
        Long serviceId,
        String serviceNome,
        Long profissionalId,
        String profissionalNome,
        OffsetDateTime dataHora,
        SlotStatus status
) {

    public static SlotResponse from(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getService().getId(),
                slot.getService().getNome(),
                slot.getProfissional().getId(),
                slot.getProfissional().getNome(),
                slot.getDataHora(),
                slot.getStatus());
    }

}
