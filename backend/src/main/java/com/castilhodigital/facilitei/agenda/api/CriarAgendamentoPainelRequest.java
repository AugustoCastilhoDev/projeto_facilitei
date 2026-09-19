package com.castilhodigital.facilitei.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Agendamento criado pelo dono/funcionario (cliente que ligou ou chegou no balcao). O profissional e obrigatorio
 * e o horario nao precisa estar na grade nem dentro do expediente - vale a palavra do dono.
 */
public record CriarAgendamentoPainelRequest(

        @NotEmpty(message = "Escolha pelo menos um serviço.")
        @Size(max = 10, message = "Escolha no máximo 10 serviços.")
        List<@NotNull(message = "Serviço inválido.") Long> servicoIds,

        @NotNull(message = "Escolha o profissional.")
        Long profissionalId,

        @NotNull(message = "Informe o horário.")
        OffsetDateTime inicio,

        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String clienteNome,

        @NotBlank(message = "Informe o telefone do cliente.")
        @Size(max = 30, message = "Telefone inválido.")
        String clienteTelefone

) {
}
