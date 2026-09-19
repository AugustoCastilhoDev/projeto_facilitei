package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.castilhodigital.facilitei.agenda.AgendamentoEvento.Tipo;
import com.castilhodigital.facilitei.notification.NotificationService;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AgendamentoNotificacaoListenerTest {

    /** 13:00Z = 10:00 em Sao Paulo (UTC-3). */
    private static final Instant INICIO = Instant.parse("2026-10-01T13:00:00Z");

    private AgendamentoEvento evento(Tipo tipo) {
        return new AgendamentoEvento(tipo, 1L, 1L, "Barbearia do Zé", ZoneId.of("America/Sao_Paulo"),
                "João", "5563999999999", INICIO, Instant.parse("2026-10-01T12:15:00Z"), "pay_1", false);
    }

    @Test
    void mensagensUsamOFusoDoNegocioEFalamComOCliente() {
        assertThat(AgendamentoNotificacaoListener.mensagem(evento(Tipo.CONFIRMADO)))
                .isEqualTo("Olá, João! Seu horário na Barbearia do Zé está confirmado: 01/10 às 10:00.");
        assertThat(AgendamentoNotificacaoListener.mensagem(evento(Tipo.AGUARDANDO_PAGAMENTO)))
                .contains("Reservamos seu horário na Barbearia do Zé (01/10 às 10:00)")
                .contains("Pague o sinal via Pix até às 09:15");
        assertThat(AgendamentoNotificacaoListener.mensagem(evento(Tipo.EXPIRADO)))
                .contains("prazo para pagar o sinal").contains("faça uma nova reserva");
        assertThat(AgendamentoNotificacaoListener.mensagem(evento(Tipo.CANCELADO_PELO_NEGOCIO)))
                .isEqualTo("Seu horário na Barbearia do Zé (01/10 às 10:00) foi cancelado pelo estabelecimento.");
    }

    @Test
    void falhaNoEnvioNaoPropagaPoisOCommitJaAconteceu() {
        NotificationService quebrado = mock(NotificationService.class);
        doThrow(new RuntimeException("MyZap fora do ar")).when(quebrado).enviar(anyString(), anyString());

        assertThatCode(() -> new AgendamentoNotificacaoListener(quebrado).aoMudarDeEstado(evento(Tipo.CONFIRMADO)))
                .doesNotThrowAnyException();
    }

}
