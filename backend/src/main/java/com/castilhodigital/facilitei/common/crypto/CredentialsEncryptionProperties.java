package com.castilhodigital.facilitei.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facilitei.security")
public record CredentialsEncryptionProperties(String credentialsEncryptionKey, String credentialsEncryptionSalt) {
}
