package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.booking.Booking;
import com.castilhodigital.facilitei.booking.PaymentStatus;
import java.time.OffsetDateTime;

/**
 * bookingId/clienteNome/clienteTelefone/statusReserva/compareceu vem de
 * Booking, nao de Slot - Slot nao tem associacao com Booking (evita
 * acoplamento ciclico de entidade entre os pacotes scheduling/booking), entao
 * o merge e feito no controller (ver SlotAdminController.listarAgenda).
 */
public record SlotResponse(
        Long id,
        Long serviceId,
        String serviceNome,
        Long profissionalId,
        String profissionalNome,
        OffsetDateTime dataHora,
        SlotStatus status,
        Long bookingId,
        String clienteNome,
        String clienteTelefone,
        PaymentStatus statusReserva,
        Boolean compareceu
) {

    public static SlotResponse from(Slot slot) {
        return from(slot, null);
    }

    public static SlotResponse from(Slot slot, Booking booking) {
        return new SlotResponse(
                slot.getId(),
                slot.getService().getId(),
                slot.getService().getNome(),
                slot.getProfissional().getId(),
                slot.getProfissional().getNome(),
                slot.getDataHora(),
                slot.getStatus(),
                booking == null ? null : booking.getId(),
                booking == null ? null : booking.getClienteNome(),
                booking == null ? null : booking.getClienteTelefone(),
                booking == null ? null : booking.getStatusPagamento(),
                booking == null ? null : booking.getCompareceu());
    }

}
