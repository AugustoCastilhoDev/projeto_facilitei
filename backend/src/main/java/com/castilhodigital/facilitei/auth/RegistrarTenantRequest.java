package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.billing.Plano;
import com.castilhodigital.facilitei.common.validation.CpfCnpj;
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
        String senhaAdmin,

        @NotBlank(message = "CPF/CNPJ e obrigatorio.")
        @CpfCnpj
        String cpfCnpj,

        @NotNull(message = "Plano e obrigatorio.")
        Plano plano,

        /** WhatsApp do dono, opcional no cadastro (pode ser informado depois, em Recebimento): e para onde vao os pedidos de confirmacao do Pix. */
        @Size(max = 30, message = "Telefone longo demais.")
        String telefoneDono

) {

    /** Cadastro sem o WhatsApp do dono. */
    public RegistrarTenantRequest(String nomeNegocio, String slug, LocalTime horarioAbertura, LocalTime horarioFechamento,
                                  String emailAdmin, String senhaAdmin, String cpfCnpj, Plano plano) {
        this(nomeNegocio, slug, horarioAbertura, horarioFechamento, emailAdmin, senhaAdmin, cpfCnpj, plano, null);
    }

}
