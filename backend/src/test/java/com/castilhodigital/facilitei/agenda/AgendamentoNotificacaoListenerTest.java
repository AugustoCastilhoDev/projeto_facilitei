package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.castilhodigital.facilitei.agenda.AgendamentoEvento.Tipo;
import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.notification.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgendamentoNotificacaoListenerTest {

    /** 13:00Z = 10:00 em Sao Paulo (UTC-3). */
    private static final Instant INICIO = Instant.parse("2026-10-01T13:00:00Z");
    private static final String TOKEN = "T".repeat(43);

    private final NotificationService whatsapp = mock(NotificationService.class);
    private final AgendamentoNotificacaoListener listener =
            new AgendamentoNotificacaoListener(whatsapp, new AvisoAoDono(whatsapp), "https://app.facilitei.com.br/");

    private AgendamentoEvento evento(Tipo tipo, MeioCobranca meio, boolean pagamentoInformado, String telefoneDono) {
        return new AgendamentoEvento(tipo, 42L, 1L, "Barbearia do Zé", telefoneDono, ZoneId.of("America/Sao_Paulo"),
                "João", "5563999999999", INICIO, Instant.parse("2026-10-01T12:15:00Z"), new BigDecimal("15.00"),
                List.of("Corte", "Barba"), meio, meio == MeioCobranca.ASAAS ? "pay_1" : null, false,
                pagamentoInformado, tipo == Tipo.PAGAMENTO_INFORMADO ? TOKEN : null);
    }

    private AgendamentoEvento evento(Tipo tipo) {
        return evento(tipo, MeioCobranca.ASAAS, false, "5563977776666");
    }

    @Test
    void mensagensAoClienteUsamOFusoDoNegocio() {
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

    /** No Pix proprio o cliente precisa saber que deve avisar que pagou - senao o dono nunca e chamado. */
    @Test
    void pedidoDePagamentoNoPixProprioExplicaOBotaoJaPaguei() {
        String mensagem = AgendamentoNotificacaoListener
                .mensagem(evento(Tipo.AGUARDANDO_PAGAMENTO, MeioCobranca.PIX_PROPRIO, false, "5563977776666"));

        assertThat(mensagem)
                .contains("Pague o sinal de R$ 15,00 via Pix até às 09:15")
                .contains("\"Já paguei\"")
                .contains("estabelecimento confirmar");
    }

    @Test
    void expirarDepoisDeOClienteAvisarQuePagouNaoDizQueEleNaoPagou() {
        String mensagem = AgendamentoNotificacaoListener
                .mensagem(evento(Tipo.EXPIRADO, MeioCobranca.PIX_PROPRIO, true, "5563977776666"));

        assertThat(mensagem)
                .contains("não confirmou o seu pagamento")
                .contains("Se você pagou, entre em contato")
                .doesNotContain("faça uma nova reserva");
    }

    @Test
    void recusaDoDonoAvisaOClienteEMandaFalarComOEstabelecimento() {
        assertThat(AgendamentoNotificacaoListener.mensagem(evento(Tipo.PAGAMENTO_NAO_LOCALIZADO)))
                .contains("não encontrou o pagamento do sinal")
                .contains("(01/10 às 10:00)")
                .contains("entre em contato com o estabelecimento");
    }

    @Test
    void pedidoDeConferenciaVaiParaODonoComOLinkSecretoAReferenciaEOValor() {
        String mensagem = listener.mensagemParaODono(evento(Tipo.PAGAMENTO_INFORMADO, MeioCobranca.PIX_PROPRIO, true, "5563977776666"));

        assertThat(mensagem)
                .contains("João avisou que pagou o sinal de R$ 15,00 (Corte + Barba, 01/10 às 10:00)")
                .contains("referência FAC42")
                .endsWith("\nhttps://app.facilitei.com.br/confirmar-sinal/" + TOKEN)   // sem "//" mesmo com a barra final na configuracao
                .doesNotContain("\r");
    }

    @Test
    void eventoDoDonoVaiSoParaODonoEOsDemaisSoParaOCliente() {
        listener.aoMudarDeEstado(evento(Tipo.PAGAMENTO_INFORMADO, MeioCobranca.PIX_PROPRIO, true, "5563977776666"));
        verify(whatsapp).enviar(eq("5563977776666"), contains("Pix a conferir"));
        verify(whatsapp, never()).enviar(eq("5563999999999"), anyString());

        listener.aoMudarDeEstado(evento(Tipo.CONFIRMADO));
        verify(whatsapp).enviar(eq("5563999999999"), contains("está confirmado"));
    }

    @Test
    void semWhatsAppDoDonoNaoTentaEnviarENemQuebra() {
        assertThatCode(() -> listener.aoMudarDeEstado(
                evento(Tipo.PAGAMENTO_INFORMADO, MeioCobranca.PIX_PROPRIO, true, null))).doesNotThrowAnyException();

        verify(whatsapp, never()).enviar(anyString(), anyString());
    }

    @Test
    void mensagemDoDonoNaoSeMisturaComAsDoCliente() {
        assertThatThrownBy(() -> AgendamentoNotificacaoListener.mensagem(evento(Tipo.PAGAMENTO_INFORMADO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.mensagemParaODono(evento(Tipo.CONFIRMADO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void falhaNoEnvioNaoPropagaPoisOCommitJaAconteceu() {
        NotificationService quebrado = mock(NotificationService.class);
        doThrow(new RuntimeException("MyZap fora do ar")).when(quebrado).enviar(anyString(), anyString());
        AgendamentoNotificacaoListener comFalha =
                new AgendamentoNotificacaoListener(quebrado, new AvisoAoDono(quebrado), "https://app.facilitei.com.br");

        assertThatCode(() -> comFalha.aoMudarDeEstado(evento(Tipo.CONFIRMADO))).doesNotThrowAnyException();
        assertThatCode(() -> comFalha.aoMudarDeEstado(
                evento(Tipo.PAGAMENTO_INFORMADO, MeioCobranca.PIX_PROPRIO, true, "5563977776666"))).doesNotThrowAnyException();
    }

    /** O token e um segredo: nao pode aparecer num log que imprima o evento. */
    @Test
    void oTokenNaoApareceNoToStringDoEvento() {
        String texto = evento(Tipo.PAGAMENTO_INFORMADO, MeioCobranca.PIX_PROPRIO, true, "5563977776666").toString();

        assertThat(texto).doesNotContain(TOKEN).contains("PAGAMENTO_INFORMADO").contains("42");
    }

}
