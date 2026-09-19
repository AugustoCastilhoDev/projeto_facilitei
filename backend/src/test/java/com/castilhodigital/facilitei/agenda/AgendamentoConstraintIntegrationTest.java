package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O que garante a agenda sao as constraints do Postgres (V16), nao codigo
 * Java - entao sao testadas contra o Postgres de verdade. Cada teste aqui e um
 * cenario que a agenda antiga (slots + checagem na aplicacao) errava ou nao
 * conseguia expressar.
 */
class AgendamentoConstraintIntegrationTest extends AgendaIntegrationTest {

    private static final String SEM_SOBREPOSICAO = "ex_agendamento_sem_sobreposicao";

    /** 10:00 em Sao Paulo. */
    private static final Instant DEZ_HORAS = Instant.parse("2026-10-01T13:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant negocio;
    private Profissional ana;
    private Profissional bia;

    @BeforeEach
    void montarNegocio() {
        negocio = tenant("barbearia");
        ana = profissional(negocio, "Ana");
        bia = profissional(negocio, "Bia");
    }

    private static Instant depois(Instant base, int minutos) {
        return base.plusSeconds(minutos * 60L);
    }

    @Test
    void doisAgendamentosSobrepostosNoMesmoProfissionalSaoRecusadosPeloBanco() {
        agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        // coloracao de 90min comecando no meio do corte
        assertThatThrownBy(() -> agendamento(negocio, ana, depois(DEZ_HORAS, 15), depois(DEZ_HORAS, 105),
                AgendamentoStatus.CONFIRMADO))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(SEM_SOBREPOSICAO);
    }

    @Test
    void horariosEncostadosNoMesmoProfissionalSaoAceitos() {
        agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        assertThatCode(() -> agendamento(negocio, ana, depois(DEZ_HORAS, 30), depois(DEZ_HORAS, 60),
                AgendamentoStatus.CONFIRMADO)).doesNotThrowAnyException();
    }

    @Test
    void mesmoHorarioComOutroProfissionalEAceito() {
        agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        assertThatCode(() -> agendamento(negocio, bia, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO))
                .doesNotThrowAnyException();
    }

    /** O bug da agenda antiga: o slot voltava a ficar livre, mas o booking cancelado continuava bloqueando uma nova reserva. */
    @Test
    void cancelarLiberaOHorarioParaUmaNovaReserva() {
        Agendamento original = agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        original.setStatus(AgendamentoStatus.CANCELADO_CLIENTE);
        agendamentoRepository.saveAndFlush(original);

        assertThatCode(() -> agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.AGUARDANDO_PAGAMENTO))
                .doesNotThrowAnyException();
    }

    /** Pagamento que chega depois de o horario ter sido liberado e retomado: o banco impede "ressuscitar" a reserva antiga. */
    @Test
    void reviverUmAgendamentoCujoHorarioJaFoiRetomadoEhRecusado() {
        Agendamento expirado = agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.EXPIRADO);
        agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        expirado.setStatus(AgendamentoStatus.CONFIRMADO);

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(expirado))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(SEM_SOBREPOSICAO);
    }

    /** Protege contra a Java e o SQL discordarem sobre quais status seguram a agenda (a lista esta escrita nos dois lugares). */
    @ParameterizedTest
    @EnumSource(AgendamentoStatus.class)
    void aListaDeStatusQueOcupamHorarioDoBancoBateComAoEnum(AgendamentoStatus status) {
        agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);
        Agendamento sobreposto = novoAgendamento(negocio, ana, depois(DEZ_HORAS, 10), depois(DEZ_HORAS, 40), status);

        if (status.ocupaHorario()) {
            assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(sobreposto))
                    .as("%s deveria ocupar o horario e ser recusado", status)
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(SEM_SOBREPOSICAO);
        } else {
            assertThatCode(() -> agendamentoRepository.saveAndFlush(sobreposto))
                    .as("%s nao ocupa horario e deveria ser aceito", status)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void agendamentoAguardandoSinalSemPrazoEhRecusado() {
        Agendamento semPrazo = novoAgendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        semPrazo.setExpiraEm(null);

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(semPrazo))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_agendamentos_espera_tem_prazo");
    }

    @Test
    void agendamentoNaoPodeApontarParaProfissionalDeOutroNegocio() {
        Tenant outroNegocio = tenant("concorrente");
        Profissional profissionalDoOutro = profissional(outroNegocio, "Carla");

        Agendamento cruzado = novoAgendamento(negocio, profissionalDoOutro, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(cruzado))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_agendamentos_profissional");
    }

    @Test
    void periodoInvalidoESinalMaiorQueOTotalSaoRecusados() {
        Agendamento fimAntesDoInicio = novoAgendamento(negocio, ana, depois(DEZ_HORAS, 30), DEZ_HORAS, AgendamentoStatus.CONFIRMADO);
        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(fimAntesDoInicio))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_agendamentos_periodo");

        Agendamento sinalAcimaDoTotal = novoAgendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);
        sinalAcimaDoTotal.setValorSinal(new BigDecimal("60.00"));
        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(sinalAcimaDoTotal))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_agendamentos_valores");
    }

    @Test
    void cadaAgendamentoTemUmCodigoPublicoProprio() {
        Agendamento primeiro = agendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);
        Agendamento segundo = agendamento(negocio, ana, depois(DEZ_HORAS, 30), depois(DEZ_HORAS, 60), AgendamentoStatus.CONFIRMADO);

        assertThat(primeiro.getCodigoPublico()).isNotNull().isNotEqualTo(segundo.getCodigoPublico());
        assertThat(agendamentoRepository.findById(primeiro.getId()).orElseThrow().getCodigoPublico())
                .isEqualTo(primeiro.getCodigoPublico());
    }

    /** O historico do cliente e o faturamento nao podem mudar quando o dono reajusta o preco de um servico. */
    @Test
    void servicosDoAgendamentoGuardamCopiaDoPrecoEDaDuracao() {
        ServiceOffering corte = servico(negocio, "Corte", 30, "50.00");
        ServiceOffering barba = servico(negocio, "Barba", 20, "35.00");

        Agendamento novo = novoAgendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 50), AgendamentoStatus.CONFIRMADO);
        novo.adicionarServico(AgendamentoServico.copiaDe(corte, 1));
        novo.adicionarServico(AgendamentoServico.copiaDe(barba, 2));
        agendamentoRepository.saveAndFlush(novo);

        corte.setPreco(new BigDecimal("80.00"));
        serviceOfferingRepository.saveAndFlush(corte);

        List<String> gravados = jdbcTemplate.query(
                "SELECT nome || ':' || preco || ':' || duracao_min FROM agendamento_servicos WHERE agendamento_id = ? ORDER BY ordem",
                (rs, i) -> rs.getString(1), novo.getId());
        assertThat(gravados).containsExactly("Corte:50.00:30", "Barba:35.00:20");
    }

    @Test
    void excluirOServicoNaoApagaOHistoricoDoAgendamento() {
        ServiceOffering corte = servico(negocio, "Corte", 30, "50.00");
        Agendamento novo = novoAgendamento(negocio, ana, DEZ_HORAS, depois(DEZ_HORAS, 30), AgendamentoStatus.CONFIRMADO);
        novo.adicionarServico(AgendamentoServico.copiaDe(corte, 1));
        agendamentoRepository.saveAndFlush(novo);

        jdbcTemplate.update("DELETE FROM profissional_servicos WHERE service_id = ?", corte.getId());
        jdbcTemplate.update("DELETE FROM services WHERE id = ?", corte.getId());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT nome FROM agendamento_servicos WHERE agendamento_id = ?", String.class, novo.getId()))
                .isEqualTo("Corte");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT servico_id FROM agendamento_servicos WHERE agendamento_id = ?", Long.class, novo.getId()))
                .isNull();
    }

    @Test
    void expedienteDoMesmoDiaNaoPodeSeSobreporMasPausaEOutrosDiasSaoLivres() {
        horario(ana, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThatThrownBy(() -> horario(ana, DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_horarios_trabalho_sem_sobreposicao");

        assertThatCode(() -> horario(ana, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(18, 0)))
                .as("encostado no fim do intervalo anterior").doesNotThrowAnyException();
        assertThatCode(() -> horario(ana, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)))
                .as("outro dia da semana").doesNotThrowAnyException();
        assertThatCode(() -> horario(bia, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)))
                .as("outro profissional").doesNotThrowAnyException();
    }

    @Test
    void expedienteComFimAntesDoInicioEhRecusado() {
        assertThatThrownBy(() -> horario(ana, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_horarios_trabalho_periodo");
    }

}
