package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.cliente.Cliente;
import java.time.Instant;
import java.util.List;

/**
 * Pedido de agendamento, ja liberto de HTTP (quem monta e o controller).
 *
 * @param profissionalId nulo = "qualquer profissional" (so no fluxo publico/portal; o painel exige um)
 * @param cliente        conta do portal, quando houver; nulo no link direto e no painel
 */
public record NovoAgendamento(
        Long tenantId,
        List<Long> servicoIds,
        Long profissionalId,
        Instant inicio,
        String clienteNome,
        String clienteTelefone,
        String clienteCpfCnpj,
        Cliente cliente,
        AgendamentoOrigem origem
) {
}
