package com.castilhodigital.facilitei.booking;

public record BookingResponse(
        Long id,
        Long slotId,
        String clienteNome,
        String clienteTelefone,
        PaymentStatus statusPagamento,
        String asaasPaymentId,
        String asaasPixPayload,
        /** So vem preenchido na resposta de criacao (ver CheckoutResult) - nao e persistido. */
        String asaasPixQrCodeBase64
) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getSlot().getId(),
                booking.getClienteNome(),
                booking.getClienteTelefone(),
                booking.getStatusPagamento(),
                booking.getAsaasPaymentId(),
                booking.getAsaasPixPayload(),
                null);
    }

    public static BookingResponse from(CheckoutResult checkoutResult) {
        Booking booking = checkoutResult.booking();
        return new BookingResponse(
                booking.getId(),
                booking.getSlot().getId(),
                booking.getClienteNome(),
                booking.getClienteTelefone(),
                booking.getStatusPagamento(),
                booking.getAsaasPaymentId(),
                booking.getAsaasPixPayload(),
                checkoutResult.qrCodeImagemBase64());
    }

}
