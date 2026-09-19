package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Maquina de estados do agendamento, sem banco: as regras de transicao ficam na propria entidade. */
class AgendamentoTest {

    private static final Instant INICIO = Instant.parse("2026-10-01T13:00:00Z");
    private static final Instant ANTES_DO_INICIO = INICIO.minusSeconds(3600);
    private static final Instant DEPOIS_DO_INICIO = INICIO.plusSeconds(60);

    private Agendamento agendamentoCom(AgendamentoStatus status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setInicio(INICIO);
        agendamento.setFim(INICIO.plusSeconds(1800));
        agendamento.setStatus(status);
        if (status.aguardaSinal()) {
            agendamento.setExpiraEm(ANTES_DO_INICIO);
        }
        return agendamento;
    }

    @Test
    void confirmarSinalLevaDeAguardandoParaConfirmadoELimpaOPrazo() {
        Agendamento agendamento = agendamentoCom(AgendamentoStatus.AGUARDANDO_PAGAMENTO);

        agendamento.confirmarSinal(ANTES_DO_INICIO);

        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(agendamento.getSinalConfirmadoEm()).isEqualTo(ANTES_DO_INICIO);
        assertThat(agendamento.getExpiraEm()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CONFIRMADO", "CONCLUIDO", "FALTOU"})
    void confirmarSinalEIdempotenteParaQuemJaPassouDessaEtapa(AgendamentoStatus jaAvancado) {
        Agendamento agendamento = agendamentoCom(jaAvancado);

        assertThatCode(() -> agendamento.confirmarSinal(ANTES_DO_INICIO)).doesNotThrowAnyException();

        assertThat(agendamento.getStatus()).as("um webhook repetido nao pode mexer no status").isEqualTo(jaAvancado);
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"EXPIRADO", "CANCELADO_CLIENTE", "CANCELADO_NEGOCIO"})
    void confirmarSinalRecusaQuemJaSaiuDaAgendaPeloFluxoNormal(AgendamentoStatus encerrado) {
        Agendamento agendamento = agendamentoCom(encerrado);

        assertThatThrownBy(() -> agendamento.confirmarSinal(ANTES_DO_INICIO)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void pagamentoTardioReativaSoAgendamentoExpirado() {
        Agendamento expirado = agendamentoCom(AgendamentoStatus.EXPIRADO);

        expirado.reativarPorPagamentoTardio(ANTES_DO_INICIO);

        assertThat(expirado.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);

        Agendamento cancelado = agendamentoCom(AgendamentoStatus.CANCELADO_NEGOCIO);
        assertThatThrownBy(() -> cancelado.reativarPorPagamentoTardio(ANTES_DO_INICIO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"AGUARDANDO_PAGAMENTO", "AGUARDANDO_CONFIRMACAO", "CONFIRMADO"})
    void negocioPodeCancelarEnquantoOAtendimentoNaoAconteceu(AgendamentoStatus status) {
        Agendamento agendamento = agendamentoCom(status);

        agendamento.cancelarPeloNegocio();

        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        assertThat(agendamento.getExpiraEm()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AgendamentoStatus.class, names = {"CONCLUIDO", "FALTOU", "EXPIRADO", "CANCELADO_CLIENTE", "CANCELADO_NEGOCIO"})
    void negocioNaoCancelaOQueJaTerminou(AgendamentoStatus status) {
        Agendamento agendamento = agendamentoCom(status);

        assertThatThrownBy(agendamento::cancelarPeloNegocio).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void concluirEMarcarFaltaExigemConfirmadoEHorarioJaIniciado() {
        Agendamento agendamento = agendamentoCom(AgendamentoStatus.CONFIRMADO);

        assertThatThrownBy(() -> agendamento.concluir(ANTES_DO_INICIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("ainda não começou");
        assertThatThrownBy(() -> agendamento.marcarFalta(ANTES_DO_INICIO))
                .isInstanceOf(RegraDeNegocioException.class);

        agendamento.concluir(DEPOIS_DO_INICIO);
        assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.CONCLUIDO);

        Agendamento faltou = agendamentoCom(AgendamentoStatus.CONFIRMADO);
        faltou.marcarFalta(DEPOIS_DO_INICIO);
        assertThat(faltou.getStatus()).isEqualTo(AgendamentoStatus.FALTOU);
    }

    @Test
    void naoSeConcluiOQueAindaNaoFoiConfirmado() {
        Agendamento agendamento = agendamentoCom(AgendamentoStatus.AGUARDANDO_PAGAMENTO);

        assertThatThrownBy(() -> agendamento.concluir(DEPOIS_DO_INICIO)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void expirarSoQuandoEstaEsperandoEOPrazoAcabou() {
        Agendamento vencido = agendamentoCom(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        vencido.setExpiraEm(INICIO);

        assertThat(vencido.expirarSeVencido(INICIO.minusSeconds(1))).as("prazo ainda nao chegou").isFalse();
        assertThat(vencido.getStatus()).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);

        assertThat(vencido.expirarSeVencido(INICIO)).as("no instante exato o prazo ja acabou").isTrue();
        assertThat(vencido.getStatus()).isEqualTo(AgendamentoStatus.EXPIRADO);

        assertThat(vencido.expirarSeVencido(INICIO.plusSeconds(60))).as("nao expira duas vezes").isFalse();
    }

    @Test
    void confirmadoNuncaExpira() {
        Agendamento confirmado = agendamentoCom(AgendamentoStatus.CONFIRMADO);
        confirmado.setExpiraEm(ANTES_DO_INICIO);

        assertThat(confirmado.expirarSeVencido(DEPOIS_DO_INICIO)).isFalse();
        assertThat(confirmado.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

    @Test
    void falhaNaCobrancaLiberaOHorarioMasNaoMexeEmConfirmado() {
        Agendamento aguardando = agendamentoCom(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        aguardando.liberarPorFalhaNaCobranca();
        assertThat(aguardando.getStatus()).isEqualTo(AgendamentoStatus.EXPIRADO);

        Agendamento confirmado = agendamentoCom(AgendamentoStatus.CONFIRMADO);
        confirmado.liberarPorFalhaNaCobranca();
        assertThat(confirmado.getStatus()).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

}
