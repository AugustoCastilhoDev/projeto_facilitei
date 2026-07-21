package com.castilhodigital.facilitei.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarBookingRequest(

        @NotNull(message = "Slot e obrigatorio.")
        Long slotId,

        @NotBlank(message = "Nome do cliente e obrigatorio.")
        String clienteNome,

        @NotBlank(message = "Telefone do cliente e obrigatorio.")
        String clienteTelefone,

        /**
         * A Asaas nao exige CPF/CNPJ para criar o cliente, mas exige para
         * criar a cobranca em si (erro "invalid_object" sem ele) - confirmado
         * testando contra o sandbox real. Por isso e obrigatorio aqui, mesmo
         * a coluna no banco sendo nullable (ver migration V6).
         */
        @NotBlank(message = "CPF/CNPJ do cliente e obrigatorio para gerar a cobranca Pix.")
        String clienteCpfCnpj

) {
}
