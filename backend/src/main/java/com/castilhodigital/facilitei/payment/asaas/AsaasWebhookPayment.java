package com.castilhodigital.facilitei.payment.asaas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsaasWebhookPayment(String id, String status) {
}
