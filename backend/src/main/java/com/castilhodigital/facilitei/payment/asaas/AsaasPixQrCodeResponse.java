package com.castilhodigital.facilitei.payment.asaas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** encodedImage = PNG em base64; payload = Pix "copia e cola". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasPixQrCodeResponse(String encodedImage, String payload, String expirationDate) {
}
