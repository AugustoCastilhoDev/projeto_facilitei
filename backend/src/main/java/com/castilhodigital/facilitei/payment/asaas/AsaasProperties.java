package com.castilhodigital.facilitei.payment.asaas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facilitei.asaas")
public record AsaasProperties(String baseUrl, String apiKey) {
}
