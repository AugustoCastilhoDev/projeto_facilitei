package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limite por IP nas rotas publicas da agenda que mudam alguma coisa: reservar (segura um horario) e "ja paguei"
 * (manda uma mensagem ao WhatsApp do dono). Sem ele, uma pessoa so, trocando o telefone digitado a cada tentativa,
 * esgotaria a agenda de um negocio ou encheria o WhatsApp do dono - o teto de 2 reservas em espera por telefone
 * nao segura quem varia o telefone.
 *
 * Os numeros padrao (10 por IP a cada 10 minutos) sobram para uma pessoa, ou uma familia no mesmo Wi-Fi, marcando
 * horarios de verdade. Sao configuraveis (facilitei.agenda.limite-*).
 */
@Component
public class LimitesDaAgendaPublica {

    static final Duration JANELA = Duration.ofMinutes(10);

    private final JanelaDeLimite reservas;
    private final JanelaDeLimite avisosDePagamento;

    public LimitesDaAgendaPublica(
            Clock clock,
            @Value("${facilitei.agenda.limite-reservas-por-ip:10}") int maximoDeReservas,
            @Value("${facilitei.agenda.limite-avisos-de-pagamento-por-ip:10}") int maximoDeAvisosDePagamento) {
        this.reservas = new JanelaDeLimite(maximoDeReservas, JANELA, clock);
        this.avisosDePagamento = new JanelaDeLimite(maximoDeAvisosDePagamento, JANELA, clock);
    }

    /** @throws LimiteDeRequisicoesExcedidoException (429) se este IP ja reservou demais na janela */
    public void contarReserva(String ip) {
        if (!reservas.permitir(ip)) {
            throw new LimiteDeRequisicoesExcedidoException(
                    "Muitas reservas em pouco tempo. Aguarde alguns minutos e tente novamente.");
        }
    }

    /** @throws LimiteDeRequisicoesExcedidoException (429) se este IP ja avisou pagamento demais na janela */
    public void contarAvisoDePagamento(String ip) {
        if (!avisosDePagamento.permitir(ip)) {
            throw new LimiteDeRequisicoesExcedidoException(
                    "Muitos avisos de pagamento em pouco tempo. Aguarde alguns minutos e tente novamente.");
        }
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    void limparEntradasExpiradas() {
        reservas.limparExpirados();
        avisosDePagamento.limparExpirados();
    }

}
