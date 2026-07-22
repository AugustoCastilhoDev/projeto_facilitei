package com.castilhodigital.facilitei.booking;

/**
 * O QR Code (base64) e retornado ao cliente na hora, mas nao e persistido no
 * Booking - so o payload "copia e cola" e o id do pagamento sao guardados
 * (ver Booking/BookingService). Se o cliente recarregar a pagina, a imagem e
 * rebuscada no Asaas por BookingCheckoutService.buscarStatusAtual.
 */
public record CheckoutResult(Booking booking, String qrCodeImagemBase64) {
}
