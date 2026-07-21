package com.castilhodigital.facilitei.payment;

public record PixChargeResult(
        String paymentId,
        /** Pix "copia e cola". */
        String payload,
        /** Imagem do QR Code em base64 (PNG), pronta para exibir num &lt;img src="data:image/png;base64,..."&gt;. */
        String qrCodeImagemBase64
) {
}
