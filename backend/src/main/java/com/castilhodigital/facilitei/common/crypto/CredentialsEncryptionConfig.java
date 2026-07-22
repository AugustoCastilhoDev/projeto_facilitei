package com.castilhodigital.facilitei.common.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Criptografia em repouso para credenciais de terceiros guardadas no banco -
 * hoje, a chave Asaas de cada tenant no modelo "traga sua propria conta de
 * pagamento" (ver EncryptedStringConverter e Tenant.asaasApiKey). A
 * chave/sal vem de variavel de ambiente; os valores default abaixo servem so
 * para desenvolvimento local, nunca para producao.
 */
@Configuration
@EnableConfigurationProperties(CredentialsEncryptionProperties.class)
@RequiredArgsConstructor
public class CredentialsEncryptionConfig {

    private final CredentialsEncryptionProperties properties;

    @Bean
    public TextEncryptor credentialsEncryptor() {
        return Encryptors.text(properties.credentialsEncryptionKey(), properties.credentialsEncryptionSalt());
    }

}
