package com.castilhodigital.facilitei.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sem campo de telefone de proposito: o telefone da reserva e sempre o do
 * Cliente autenticado (ver PortalBookingController), nunca confiado no corpo
 * da requisicao.
 */
public record CriarReservaPortalRequest(

        @NotNull(message = "Slot e obrigatorio.")
        Long slotId,

        @NotBlank(message = "Nome do cliente e obrigatorio.")
        String clienteNome,

        @NotBlank(message = "CPF/CNPJ do cliente e obrigatorio para gerar a cobranca Pix.")
        String clienteCpfCnpj

) {
}
