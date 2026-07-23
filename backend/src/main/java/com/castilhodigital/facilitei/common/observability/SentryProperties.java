package com.castilhodigital.facilitei.common.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facilitei.sentry")
public record SentryProperties(String dsn, String environment) {
}
