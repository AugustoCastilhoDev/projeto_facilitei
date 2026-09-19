package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.agenda.DisponibilidadeService.HorarioDisponivel;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Disponibilidade de ponta a ponta contra o Postgres: expediente, bloqueios e
 * agendamentos reais, parametros do negocio e a regra de "qualquer profissional".
 *
 * Cenario: Ana faz Corte e Barba (seg 09-12 e 14-18); Bia so faz Corte (seg 10-16).
 * Todos os testes olham a segunda-feira 2026-09-21; o "agora" e o domingo anterior,
 * entao antecedencia e horizonte nao interferem, exceto nos testes que os exercitam.
 */
class DisponibilidadeServiceIntegrationTest extends AgendaIntegrationTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 21);
    private static final Instant DOMINGO_ANTERIOR = LocalDate.of(2026, 9, 20).atTime(12, 0).atZone(SAO_PAULO).toInstant();

    @Autowired
    private DisponibilidadeService disponibilidadeService;

    private Tenant negocio;
    private ServiceOffering corte;
    private ServiceOffering barba;
    private Profissional ana;
    private Profissional bia;

    @BeforeEach
    void montarCenario() {
        negocio = tenant("barbearia");
        corte = servico(negocio, "Corte", 30, "50.00");
        barba = servico(negocio, "Barba", 30, "35.00");
        ana = profissional(negocio, "Ana", corte, barba);
        bia = profissional(negocio, "Bia", corte);

        horario(ana, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        horario(ana, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0));
        horario(bia, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(16, 0));
    }

    private static Instant sp(int hora, int minuto) {
        return SEGUNDA.atTime(hora, minuto).atZone(SAO_PAULO).toInstant();
    }

    private List<HorarioDisponivel> listar(List<Long> servicoIds, Long profissionalId) {
        return disponibilidadeService.listar(negocio.getId(), servicoIds, profissionalId, SEGUNDA, DOMINGO_ANTERIOR);
    }

    private static List<Instant> inicios(List<HorarioDisponivel> horarios) {
        return horarios.stream().map(HorarioDisponivel::inicio).toList();
    }

    @Test
    void profissionalEspecificoRecebeOsHorariosDoProprioExpediente() {
        List<HorarioDisponivel> horarios = listar(List.of(corte.getId()), ana.getId());

        assertThat(horarios).hasSize(14);
        assertThat(inicios(horarios)).startsWith(sp(9, 0)).contains(sp(11, 30), sp(14, 0)).endsWith(sp(17, 30));
        assertThat(horarios).allSatisfy(horario -> {
            assertThat(horario.profissionalId()).isEqualTo(ana.getId());
            assertThat(Duration.between(horario.inicio(), horario.fim())).isEqualTo(Duration.ofMinutes(30));
        });
    }

    @Test
    void agendamentoAtivoBloqueiaSoOSeuHorario() {
        agendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.CONFIRMADO);

        List<Instant> inicios = inicios(listar(List.of(corte.getId()), ana.getId()));

        assertThat(inicios).doesNotContain(sp(10, 0)).contains(sp(9, 30), sp(10, 30));
    }

    @Test
    void agendamentoQueSegurouAAgendaEsperandoPagamentoTambemBloqueia() {
        agendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.AGUARDANDO_PAGAMENTO);

        assertThat(inicios(listar(List.of(corte.getId()), ana.getId()))).doesNotContain(sp(10, 0));
    }

    @Test
    void agendamentoExpiradoOuCanceladoLiberaOHorario() {
        agendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.EXPIRADO);
        agendamento(negocio, ana, sp(11, 0), sp(11, 30), AgendamentoStatus.CANCELADO_CLIENTE);
        agendamento(negocio, ana, sp(9, 0), sp(9, 30), AgendamentoStatus.CANCELADO_NEGOCIO);

        assertThat(inicios(listar(List.of(corte.getId()), ana.getId()))).contains(sp(10, 0), sp(11, 0), sp(9, 0));
    }

    @Test
    void agendamentoDeOutroProfissionalNaoAfetaEste() {
        agendamento(negocio, bia, sp(10, 0), sp(10, 30), AgendamentoStatus.CONFIRMADO);

        assertThat(inicios(listar(List.of(corte.getId()), ana.getId()))).contains(sp(10, 0));
    }

    @Test
    void bloqueioTiraOPeriodoDaAgenda() {
        bloqueio(ana, sp(14, 0), sp(16, 0));

        List<Instant> inicios = inicios(listar(List.of(corte.getId()), ana.getId()));

        assertThat(inicios).doesNotContain(sp(14, 0), sp(15, 30)).contains(sp(11, 30), sp(16, 0));
    }

    @Test
    void bloqueioLongoQueComecouAntesDoDiaAindaValeParaODia() {
        // ferias de uma semana que comecaram na semana passada
        bloqueio(ana, sp(9, 0).minusSeconds(7 * 24 * 3600L), sp(9, 0).plusSeconds(3 * 24 * 3600L));

        assertThat(listar(List.of(corte.getId()), ana.getId())).isEmpty();
    }

    @Test
    void variosServicosSomamADuracaoDoAtendimento() {
        List<HorarioDisponivel> horarios = listar(List.of(corte.getId(), barba.getId()), ana.getId());

        // 60min: manha 09:00..11:00 (5 inicios) e tarde 14:00..17:00 (7 inicios)
        assertThat(horarios).hasSize(12);
        assertThat(horarios).allSatisfy(horario ->
                assertThat(Duration.between(horario.inicio(), horario.fim())).isEqualTo(Duration.ofMinutes(60)));
        assertThat(inicios(horarios)).doesNotContain(sp(11, 30)).contains(sp(11, 0), sp(17, 0));
    }

    @Test
    void qualquerProfissionalUneAsAgendasEAtribuiOPrimeiroLivrePorNome() {
        List<HorarioDisponivel> horarios = listar(List.of(corte.getId()), null);

        // Ana: 14 horarios. Bia acrescenta os 4 que a Ana nao tem (almoco 12:00-13:30). Sem repetir horario.
        assertThat(horarios).hasSize(18);
        assertThat(inicios(horarios)).isSorted().doesNotHaveDuplicates();

        assertThat(profissionalDo(horarios, sp(10, 0))).as("os dois livres: vence a Ana (ordem por nome)").isEqualTo(ana.getId());
        assertThat(profissionalDo(horarios, sp(12, 0))).as("Ana esta no almoco").isEqualTo(bia.getId());
        assertThat(profissionalDo(horarios, sp(17, 0))).as("Bia ja encerrou").isEqualTo(ana.getId());
    }

    @Test
    void qualquerProfissionalPassaParaOOutroQuandoOPrimeiroEstaOcupado() {
        agendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.CONFIRMADO);

        List<HorarioDisponivel> horarios = listar(List.of(corte.getId()), null);

        assertThat(profissionalDo(horarios, sp(10, 0))).isEqualTo(bia.getId());
    }

    @Test
    void qualquerProfissionalSoConsideraQuemRealizaTodosOsServicosPedidos() {
        List<HorarioDisponivel> horarios = listar(List.of(corte.getId(), barba.getId()), null);

        assertThat(horarios).isNotEmpty().allSatisfy(horario -> assertThat(horario.profissionalId()).isEqualTo(ana.getId()));
    }

    @Test
    void profissionalInativoNaoEhOferecidoEmQualquerProfissional() {
        ana.setAtivo(false);
        profissionalRepository.saveAndFlush(ana);

        List<HorarioDisponivel> horarios = listar(List.of(corte.getId()), null);

        assertThat(horarios).isNotEmpty().allSatisfy(horario -> assertThat(horario.profissionalId()).isEqualTo(bia.getId()));
    }

    @Test
    void profissionalQueNaoRealizaOServicoEhRecusado() {
        assertThatThrownBy(() -> listar(List.of(barba.getId()), bia.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nao realiza");
    }

    @Test
    void servicoInativoEhRecusado() {
        corte.setAtivo(false);
        serviceOfferingRepository.saveAndFlush(corte);

        assertThatThrownBy(() -> listar(List.of(corte.getId()), ana.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Corte");
    }

    @Test
    void servicoDeOutroNegocioNaoEhEncontrado() {
        Tenant concorrente = tenant("concorrente");
        ServiceOffering servicoDeOutro = servico(concorrente, "Corte", 30, "10.00");

        assertThatThrownBy(() -> listar(List.of(servicoDeOutro.getId()), null))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void semServicoNenhumEhRecusado() {
        assertThatThrownBy(() -> listar(List.of(), null)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void diaSemExpedienteNaoTemHorarios() {
        LocalDate domingo = LocalDate.of(2026, 9, 27);

        assertThat(disponibilidadeService.listar(negocio.getId(), List.of(corte.getId()), null, domingo, DOMINGO_ANTERIOR)).isEmpty();
    }

    @Test
    void antecedenciaMinimaDoNegocioConsideraOAgora() {
        // 09:30 da propria segunda, com 60min de antecedencia: o primeiro horario possivel e 10:30
        Instant agora = sp(9, 30);

        List<Instant> inicios = inicios(disponibilidadeService.listar(negocio.getId(), List.of(corte.getId()), ana.getId(), SEGUNDA, agora));

        assertThat(inicios).startsWith(sp(10, 30)).doesNotContain(sp(9, 30), sp(10, 0));
    }

    @Test
    void horizonteDoNegocioLimitaOQuantoAFrenteSePodeMarcar() {
        // hoje = domingo 2026-09-20 e o horizonte padrao e 60 dias: 2026-11-16 (segunda) cabe, 2026-11-30 (segunda) nao
        LocalDate dentro = LocalDate.of(2026, 11, 16);
        LocalDate fora = LocalDate.of(2026, 11, 30);

        assertThat(disponibilidadeService.listar(negocio.getId(), List.of(corte.getId()), ana.getId(), dentro, DOMINGO_ANTERIOR)).isNotEmpty();
        assertThat(disponibilidadeService.listar(negocio.getId(), List.of(corte.getId()), ana.getId(), fora, DOMINGO_ANTERIOR)).isEmpty();
    }

    @Test
    void usaOFusoEAGradeConfiguradosNoNegocio() {
        negocio.setFusoHorario("America/Manaus");
        negocio.setIntervaloAgendaMin(60);
        tenantRepository.saveAndFlush(negocio);

        List<HorarioDisponivel> horarios = listar(List.of(corte.getId()), ana.getId());

        // 09:00 de Manaus (UTC-4) = 13:00Z, e a grade agora e de 60 em 60 minutos
        assertThat(inicios(horarios)).startsWith(Instant.parse("2026-09-21T13:00:00Z"));
        assertThat(inicios(horarios)).containsSequence(
                Instant.parse("2026-09-21T13:00:00Z"), Instant.parse("2026-09-21T14:00:00Z"), Instant.parse("2026-09-21T15:00:00Z"));
    }

    private static Long profissionalDo(List<HorarioDisponivel> horarios, Instant inicio) {
        return horarios.stream().filter(h -> h.inicio().equals(inicio)).findFirst().orElseThrow().profissionalId();
    }

}
