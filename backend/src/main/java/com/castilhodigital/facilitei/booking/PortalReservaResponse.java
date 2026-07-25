package com.castilhodigital.facilitei.booking;

import java.time.OffsetDateTime;

/**
 * Resposta especifica do portal do cliente - diferente de BookingResponse
 * (usada por PublicBookingController/BookingAdminController), inclui
 * negocio/servico/horario porque aqui o cliente cruza varios tenants e
 * precisa saber a qual reserva cada card se refere. Ver
 * BookingRepository.findByCliente_IdOrderBySlot_DataHoraDesc (JOIN FETCH
 * garante que slot/service/tenant ja estao carregados).
 */
public record PortalReservaResponse(
        Long id,
        String tenantNome,
        String tenantSlug,
        String serviceNome,
        OffsetDateTime dataHora,
        PaymentStatus statusPagamento,
        String asaasPaymentId,
        String asaasPixPayload,
        String asaasPixQrCodeBase64
) {

    public static PortalReservaResponse from(Booking booking) {
        return new PortalReservaResponse(
                booking.getId(),
                booking.getSlot().getTenant().getNome(),
                booking.getSlot().getTenant().getSlug(),
                booking.getSlot().getService().getNome(),
                booking.getSlot().getDataHora(),
                booking.getStatusPagamento(),
                booking.getAsaasPaymentId(),
                booking.getAsaasPixPayload(),
                null);
    }

    public static PortalReservaResponse from(CheckoutResult checkoutResult) {
        Booking booking = checkoutResult.booking();
        return new PortalReservaResponse(
                booking.getId(),
                booking.getSlot().getTenant().getNome(),
                booking.getSlot().getTenant().getSlug(),
                booking.getSlot().getService().getNome(),
                booking.getSlot().getDataHora(),
                booking.getStatusPagamento(),
                booking.getAsaasPaymentId(),
                booking.getAsaasPixPayload(),
                checkoutResult.qrCodeImagemBase64());
    }

}
