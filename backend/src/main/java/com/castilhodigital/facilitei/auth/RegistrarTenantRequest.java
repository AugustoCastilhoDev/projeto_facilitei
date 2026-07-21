package com.castilhodigital.facilitei.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record RegistrarTenantRequest(

        @NotBlank(message = "Nome do negocio e obrigatorio.")
        String nomeNegocio,

        @NotBlank(message = "Slug e obrigatorio.")
        @jakarta.validation.constraints.Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug deve conter apenas letras minusculas, numeros e hifens.")
        String slug,

        @NotNull(message = "Horario de abertura e obrigatorio.")
        LocalTime horarioAbertura,

        @NotNull(message = "Horario de fechamento e obrigatorio.")
        LocalTime horarioFechamento,

        @NotBlank(message = "Email do administrador e obrigatorio.")
        @Email(message = "Email invalido.")
        String emailAdmin,

        @NotBlank(message = "Senha e obrigatoria.")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres.")
        String senhaAdmin

) {
}
