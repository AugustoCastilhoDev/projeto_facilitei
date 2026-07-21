package com.castilhodigital.facilitei.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ServiceOfferingRequest(

        @NotBlank(message = "Nome do servico e obrigatorio.")
        String nome,

        @NotNull(message = "Duracao e obrigatoria.")
        @Positive(message = "Duracao deve ser maior que zero.")
        Integer duracaoMin,

        @NotNull(message = "Preco e obrigatorio.")
        @DecimalMin(value = "0.0", message = "Preco nao pode ser negativo.")
        BigDecimal preco,

        @NotNull(message = "Percentual de sinal e obrigatorio.")
        @DecimalMin(value = "0.0", message = "Percentual de sinal deve estar entre 0 e 100.")
        @DecimalMax(value = "100.0", message = "Percentual de sinal deve estar entre 0 e 100.")
        BigDecimal sinalPercentual

) {
}
