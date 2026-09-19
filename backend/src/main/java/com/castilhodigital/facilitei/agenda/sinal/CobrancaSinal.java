package com.castilhodigital.facilitei.agenda.sinal;

/**
 * Resultado de criar a cobranca do sinal.
 *
 * @param cobrancaExternaId id da cobranca no meio de pagamento (usado para casar o webhook); nulo se o meio nao tem
 * @param payload           Pix "copia e cola"
 * @param qrCodeBase64      imagem PNG do QR em base64, se o meio ja devolver pronta
 */
public record CobrancaSinal(String cobrancaExternaId, String payload, String qrCodeBase64) {
}
