package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.common.validation.CpfCnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pedido de agendamento feito pelo cliente na pagina publica.
 *
 * @param profissionalId nulo = "qualquer profissional"
 * @param inicio         exatamente um dos horarios devolvidos por GET .../agenda/disponibilidade
 * @param clienteCpfCnpj opcional: so e exigido (na criacao) quando o negocio recebe o sinal por um meio que precisa dele
 */
public record CriarAgendamentoRequest(

        @NotEmpty(message = "Escolha pelo menos um serviço.")
        @Size(max = 10, message = "Escolha no máximo 10 serviços.")
        List<@NotNull(message = "Serviço inválido.") Long> servicoIds,

        Long profissionalId,

        @NotNull(message = "Escolha o horário.")
        OffsetDateTime inicio,

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String clienteNome,

        @NotBlank(message = "Informe seu telefone.")
        @Size(max = 30, message = "Telefone inválido.")
        String clienteTelefone,

        @CpfCnpj
        String clienteCpfCnpj

) {
}
