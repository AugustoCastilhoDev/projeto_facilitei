package com.castilhodigital.facilitei.payment.asaas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasPaymentResponse(String id, String status) {
}
