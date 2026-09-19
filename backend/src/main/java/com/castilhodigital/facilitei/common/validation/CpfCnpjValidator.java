package com.castilhodigital.facilitei.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // nulo/vazio fica por conta do @NotBlank - aqui so o que foi informado e conferido
        return valor == null || valor.isBlank() || CpfCnpjUtils.valido(valor);
    }

}
