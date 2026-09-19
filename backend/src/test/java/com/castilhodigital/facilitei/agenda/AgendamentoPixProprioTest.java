package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * As transicoes do Pix proprio (o cliente avisa que pagou, o dono confirma ou recusa), sem banco. Cada status e
 * testado - inclusive os que nao deveriam mudar - porque o erro caro aqui e "confirmar" ou "recusar" algo que
 * nao estava esperando isso.
 */
class AgendamentoPixProprioTest {

    private static final Instant INICIO = Instant.parse("2026-10-01T13:00:00Z");
    private static final Instant AGORA = INICIO.minus(Duration.ofHours(5));
    private static final Duration UMA_HORA = Duration.ofMinutes(60);
    private static final String HASH = "h".repeat(64);

    private Agendamento agendamento(AgendamentoStatus status, MeioCobranca meio) {
        Agendamento agendamento = new Agendamento();
        agendamento.setInicio(INICIO);
        agendamento.setFim(INICIO.plusSeconds(1800));
        agendamento.setStatus(status);
        agendamento.setMeioCobranca(meio);
        if (status.aguardaSinal()) {
            agendamento.setExpiraEm(AGORA.plusSeconds(600));
        }
        if (status == AgendamentoStatus.AGUARDANDO_CONFIRMACAO) {
            agendamento.setPagamentoInformadoEm(AGORA.minusSeconds(60));
        }
        return agendamento;
    }

    private Agendamento noPixProprio(AgendamentoStatus status) {
        return agendamento(status, MeioCobranca.PIX_PROPRIO);
    }

    // ---- informarPagamento (cliente) ----

    @Test
    void informarPagamentoSeguraOHorarioPorMaisTempoEGuardaOAvisoEOToken() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.AGUARDANDO_PAGAMENTO);

        boolean mudou = agendamento.informarPagamento(AGORA, UMA_HORA, HASH);

        assertThat(mudou).isTrue();
        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        assertThat(agendamento.getPagamentoInformadoEm()).isEqualTo(AGORA);
        assertThat(agendamento.getExpiraEm()).isEqualTo(AGORA.plus(UMA_HORA));
        assertThat(agendamento.getTokenConfirmacaoHash()).isEqualTo(HASH);
    }

    /** Quem reserva 30 minutos antes nao pode segurar o horario 60 minutos: passaria da hora marcada. */
    @Test
    void oPrazoDeConfirmacaoNuncaPassaDoInicioDoAtendimento() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        Instant meiaHoraAntes = INICIO.minus(Duration.ofMinutes(30));

        agendamento.informarPagamento(meiaHoraAntes, UMA_HORA, HASH);

        assertThat(agendamento.getExpiraEm()).isEqualTo(INICIO);
    }

    @Test
    void informarDeNovoNaoMudaNadaNemGeraOutroTokenOuOutroPedidoAoDono() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        agendamento.informarPagamento(AGORA, UMA_HORA, HASH);

        boolean mudou = agendamento.informarPagamento(AGORA.plusSeconds(30), UMA_HORA, "outro".repeat(13));

        assertThat(mudou).isFalse();
        assertThat(agendamento.getTokenConfirmacaoHash()).as("o link ja enviado ao dono continua valendo").isEqualTo(HASH);
        assertThat(agendamento.getPagamentoInformadoEm()).isEqualTo(AGORA);
    }

    /** O cliente pagou mas demorou: a reserva expirou. O aviso a traz de volta (a constraint do banco decide se o horario ainda e dele). */
    @Test
    void informarDepoisDeExpirarRetomaAEsperaDeConfirmacao() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.EXPIRADO);

        boolean mudou = agendamento.informarPagamento(AGORA, UMA_HORA, HASH);

        assertThat(mudou).isTrue();
        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        assertThat(agendamento.getExpiraEm()).isEqualTo(AGORA.plus(UMA_HORA));
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CONFIRMADO", "CONCLUIDO", "FALTOU"})
    void informarQuemJaEstaConfirmadoNaoFazNada(AgendamentoStatus jaConfirmado) {
        Agendamento agendamento = noPixProprio(jaConfirmado);

        assertThat(agendamento.informarPagamento(AGORA, UMA_HORA, HASH)).isFalse();

        assertThat(agendamento.getStatus()).isEqualTo(jaConfirmado);
        assertThat(agendamento.getTokenConfirmacaoHash()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CANCELADO_CLIENTE", "CANCELADO_NEGOCIO"})
    void informarPagamentoDeAgendamentoCanceladoERecusado(AgendamentoStatus cancelado) {
        assertThatThrownBy(() -> noPixProprio(cancelado).informarPagamento(AGORA, UMA_HORA, HASH))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("cancelado");
    }

    @Test
    void informarDepoisDaHoraMarcadaERecusado() {
        assertThatThrownBy(() -> noPixProprio(AgendamentoStatus.EXPIRADO).informarPagamento(INICIO, UMA_HORA, HASH))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("já passou");
        assertThatThrownBy(() -> noPixProprio(AgendamentoStatus.AGUARDANDO_PAGAMENTO)
                .informarPagamento(INICIO.plusSeconds(1), UMA_HORA, HASH))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    /** Quem paga pela Asaas e confirmado pelo webhook: um "ja paguei" ali seria so uma forma de tentar confirmar sem pagar. */
    @Test
    void soOPixProprioTemAvisoDeQuemPagou() {
        assertThatThrownBy(() -> agendamento(AgendamentoStatus.AGUARDANDO_PAGAMENTO, MeioCobranca.ASAAS)
                .informarPagamento(AGORA, UMA_HORA, HASH))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("automaticamente");
        assertThatThrownBy(() -> agendamento(AgendamentoStatus.AGUARDANDO_PAGAMENTO, null)
                .informarPagamento(AGORA, UMA_HORA, HASH))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // ---- confirmarPeloDono ----

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"AGUARDANDO_PAGAMENTO", "AGUARDANDO_CONFIRMACAO"})
    void donoConfirmaQuemEstaEsperando(AgendamentoStatus esperando) {
        Agendamento agendamento = noPixProprio(esperando);

        assertThat(agendamento.confirmarPeloDono(AGORA)).isTrue();

        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(agendamento.getSinalConfirmadoEm()).isEqualTo(AGORA);
        assertThat(agendamento.getExpiraEm()).isNull();
    }

    /** O dono atesta que recebeu: mesmo expirada, a reserva volta (se o horario estiver livre - o banco confere ao gravar). */
    @Test
    void donoConfirmaTambemQuemJaExpirou() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.EXPIRADO);

        assertThat(agendamento.confirmarPeloDono(AGORA)).isTrue();

        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CONFIRMADO", "CONCLUIDO", "FALTOU"})
    void confirmarDeNovoEIdempotente(AgendamentoStatus jaConfirmado) {
        Agendamento agendamento = noPixProprio(jaConfirmado);

        assertThat(agendamento.confirmarPeloDono(AGORA)).isFalse();

        assertThat(agendamento.getStatus()).isEqualTo(jaConfirmado);
        assertThat(agendamento.getSinalConfirmadoEm()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CANCELADO_CLIENTE", "CANCELADO_NEGOCIO"})
    void naoSeConfirmaAgendamentoCancelado(AgendamentoStatus cancelado) {
        assertThatThrownBy(() -> noPixProprio(cancelado).confirmarPeloDono(AGORA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("cancelado");
    }

    // ---- recusarPagamento ----

    @Test
    void donoRecusaQuemAvisouQuePagouEOHorarioVoltaParaAAgenda() {
        Agendamento agendamento = noPixProprio(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);

        assertThat(agendamento.recusarPagamento()).isTrue();

        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        assertThat(agendamento.getStatus().ocupaHorario()).isFalse();
        assertThat(agendamento.getExpiraEm()).isNull();
    }

    /** O dono clicou em "nao recebi" depois de o prazo ja ter liberado o horario: nada a desfazer, e nao e erro. */
    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"EXPIRADO", "CANCELADO_CLIENTE", "CANCELADO_NEGOCIO"})
    void recusarQuemJaEstaLiberadoEIdempotente(AgendamentoStatus liberado) {
        Agendamento agendamento = noPixProprio(liberado);

        assertThat(agendamento.recusarPagamento()).isFalse();

        assertThat(agendamento.getStatus()).isEqualTo(liberado);
    }

    @Test
    void recusarAntesDeOClienteAvisarQuePagouEOrientaCancelar() {
        assertThatThrownBy(() -> noPixProprio(AgendamentoStatus.AGUARDANDO_PAGAMENTO).recusarPagamento())
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("cancele");
    }

    /** Ja confirmado: derrubar a reserva por um link antigo seria perigoso, entao so pelo painel (cancelar). */
    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CONFIRMADO", "CONCLUIDO", "FALTOU"})
    void recusarQuemJaFoiConfirmadoERecusado(AgendamentoStatus confirmado) {
        Agendamento agendamento = noPixProprio(confirmado);

        assertThatThrownBy(agendamento::recusarPagamento)
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("já foi confirmado");
        assertThat(agendamento.getStatus()).isEqualTo(confirmado);
    }

}
