package com.castilhodigital.facilitei.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O valor precisa ser um CPF ou CNPJ com digitos verificadores corretos
 * (com ou sem mascara). Nulo e considerado valido - combine com @NotBlank
 * quando o campo for obrigatorio.
 */
@Documented
@Constraint(validatedBy = CpfCnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfCnpj {

    String message() default "CPF/CNPJ invalido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
