package com.castilhodigital.facilitei.notification.myzap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facilitei.myzap")
public record MyZapProperties(String baseUrl, String apiKey) {
}
