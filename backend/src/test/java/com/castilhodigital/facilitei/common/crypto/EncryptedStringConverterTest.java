package com.castilhodigital.facilitei.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

class EncryptedStringConverterTest {

    private final TextEncryptor textEncryptor = Encryptors.text("chave-de-teste", "deadbeef");
    private final EncryptedStringConverter converter = new EncryptedStringConverter(textEncryptor);

    @Test
    void valorCifradoNaoBateComOTextoOriginal() {
        String cifrado = converter.convertToDatabaseColumn("chave-real-do-tenant");

        assertThat(cifrado).isNotEqualTo("chave-real-do-tenant");
    }

    @Test
    void decifrarORoundTripDevolveOTextoOriginal() {
        String cifrado = converter.convertToDatabaseColumn("chave-real-do-tenant");
        String decifrado = converter.convertToEntityAttribute(cifrado);

        assertThat(decifrado).isEqualTo("chave-real-do-tenant");
    }

    @Test
    void valorNuloPermaneceNulo() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

}
