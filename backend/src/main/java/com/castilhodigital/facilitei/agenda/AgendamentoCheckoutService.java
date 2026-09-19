package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinal;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalService;
import com.castilhodigital.facilitei.agenda.sinal.SolicitacaoCobranca;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalResolver;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orquestra o checkout: segura o horario, cria a cobranca do sinal e a vincula.
 *
 * Deliberadamente SEM @Transactional. Sao tres transacoes curtas e independentes
 * (AgendamentoService), com a chamada ao gateway - uma rede, que pode demorar ou
 * falhar - ENTRE elas, sem conexao do banco aberta:
 *  1. criar: grava o agendamento segurando o horario (a constraint decide a corrida);
 *  2. cobrar: cria a cobranca no gateway;
 *  3. vincular: guarda a cobranca e avisa o cliente.
 * Se o passo 2 falha, o horario e devolvido na hora (compensacao) e o erro sobe.
 * A agenda antiga fazia tudo numa transacao so, com o HTTP dentro dela.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoCheckoutService {

    private final AgendamentoService agendamentoService;
    private final CobrancaSinalResolver cobrancaSinalResolver;
    private final TenantService tenantService;

    public CheckoutAgendamento iniciar(NovoAgendamento pedido, Instant agora) {
        AgendamentoView criado = agendamentoService.criar(pedido, agora);
        if (criado.status() != AgendamentoStatus.AGUARDANDO_PAGAMENTO) {
            return new CheckoutAgendamento(criado, null);
        }

        Tenant tenant = tenantService.buscarPorId(pedido.tenantId());
        CobrancaSinalService meioDeCobranca = cobrancaSinalResolver.exigir(tenant);

        CobrancaSinal cobranca;
        try {
            cobranca = meioDeCobranca.criar(tenant, new SolicitacaoCobranca(
                    criado.id(), criado.clienteNome(), criado.clienteTelefone(), pedido.clienteCpfCnpj(), criado.valorSinal()));
        } catch (RuntimeException ex) {
            log.warn("Cobranca do sinal falhou para o agendamento {}; devolvendo o horario.", criado.id());
            agendamentoService.liberarPorFalhaNaCobranca(criado.id());
            throw ex;
        }

        AgendamentoView vinculado = agendamentoService.vincularCobranca(criado.id(), cobranca);
        return new CheckoutAgendamento(vinculado, cobranca);
    }

}
