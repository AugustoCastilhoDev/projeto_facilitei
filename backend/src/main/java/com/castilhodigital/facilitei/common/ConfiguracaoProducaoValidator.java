package com.castilhodigital.facilitei.common;

import com.castilhodigital.facilitei.common.crypto.CredentialsEncryptionProperties;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Impede a aplicacao de subir em producao com configuracao insegura. O
 * application-prod.yml ja exige que as variaveis de ambiente existam; isto
 * cobre o que ele nao consegue: alguem preencher a variavel com o valor de
 * desenvolvimento (que esta no repositorio) ou com um segredo fraco.
 *
 * Um JWT_SECRET conhecido permitiria forjar um token ADMIN de qualquer
 * negocio; uma chave de cifra conhecida deixaria as chaves Asaas dos tenants
 * (guardadas cifradas no banco) legiveis para quem tiver o dump do banco.
 */
@Component
@Profile("prod")
public class ConfiguracaoProducaoValidator {

    /** HS256 exige chave de pelo menos 256 bits. */
    static final int TAMANHO_MINIMO_SEGREDO = 32;

    /** Marca presente nos defaults de desenvolvimento do application.yml (JWT e chave de cifra). */
    private static final String MARCA_DE_SEGREDO_DE_DESENVOLVIMENTO = "local-dev";
    private static final String SALT_DE_DESENVOLVIMENTO = "deadbeefcafe0123";
    private static final int TAMANHO_MINIMO_SALT_HEX = 16;

    private final String jwtSecret;
    private final String notificationProvider;
    private final CredentialsEncryptionProperties encryption;

    public ConfiguracaoProducaoValidator(
            @Value("${facilitei.jwt.secret}") String jwtSecret,
            @Value("${facilitei.notification.provider}") String notificationProvider,
            CredentialsEncryptionProperties encryption) {
        this.jwtSecret = jwtSecret;
        this.notificationProvider = notificationProvider;
        this.encryption = encryption;
    }

    @PostConstruct
    void validar() {
        List<String> problemas = new ArrayList<>();

        validarSegredo("JWT_SECRET", jwtSecret, problemas);
        validarSegredo("CREDENTIALS_ENCRYPTION_KEY", encryption.credentialsEncryptionKey(), problemas);
        validarSalt(encryption.credentialsEncryptionSalt(), problemas);

        if ("console".equalsIgnoreCase(notificationProvider)) {
            problemas.add("NOTIFICATION_PROVIDER=console so registra as mensagens no log (inclusive o codigo OTP) - use myzap.");
        }

        if (!problemas.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracao de producao insegura, a aplicacao nao vai subir:\n - " + String.join("\n - ", problemas));
        }
    }

    private void validarSegredo(String nome, String valor, List<String> problemas) {
        if (valor == null || valor.length() < TAMANHO_MINIMO_SEGREDO) {
            problemas.add(nome + " precisa ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        } else if (valor.contains(MARCA_DE_SEGREDO_DE_DESENVOLVIMENTO)) {
            problemas.add(nome + " ainda e o valor de desenvolvimento do repositorio - gere um segredo proprio.");
        }
    }

    private void validarSalt(String salt, List<String> problemas) {
        if (salt == null || salt.length() < TAMANHO_MINIMO_SALT_HEX || !salt.matches("[0-9a-fA-F]+")
                || salt.length() % 2 != 0) {
            problemas.add("CREDENTIALS_ENCRYPTION_SALT precisa ser hexadecimal, de tamanho par, com pelo menos "
                    + TAMANHO_MINIMO_SALT_HEX + " caracteres.");
        } else if (SALT_DE_DESENVOLVIMENTO.equalsIgnoreCase(salt)) {
            problemas.add("CREDENTIALS_ENCRYPTION_SALT ainda e o valor de desenvolvimento do repositorio - gere um proprio.");
        }
    }

}
