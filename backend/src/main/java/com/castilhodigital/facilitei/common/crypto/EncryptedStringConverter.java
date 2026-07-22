package com.castilhodigital.facilitei.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Cifra/decifra de forma transparente campos de entidade marcados com
 * {@code @Convert(converter = EncryptedStringConverter.class)}: o valor em
 * memoria (na entidade Java) e sempre o texto plano, so a coluna no banco
 * fica cifrada. Registrado como bean Spring (nao instanciado pelo Hibernate
 * via reflection) para poder injetar o TextEncryptor - o Spring Boot
 * configura o Hibernate para resolver conversores assim via o proprio
 * contexto, nao so via construtor sem argumentos.
 */
@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final TextEncryptor textEncryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : textEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : textEncryptor.decrypt(dbData);
    }

}
