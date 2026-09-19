package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.agenda.DisponibilidadeCalculator.IntervaloDiario;
import com.castilhodigital.facilitei.agenda.DisponibilidadeCalculator.PeriodoOcupado;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisponibilidadeCalculatorTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 21);
    private static final Instant SEM_LIMITE_INFERIOR = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant SEM_LIMITE_SUPERIOR = Instant.parse("2100-01-01T00:00:00Z");

    private static final IntervaloDiario MANHA = new IntervaloDiario(LocalTime.of(9, 0), LocalTime.of(12, 0));
    private static final IntervaloDiario TARDE = new IntervaloDiario(LocalTime.of(14, 0), LocalTime.of(18, 0));

    /** Instante correspondente a hh:mm na segunda-feira de teste, no fuso de Sao Paulo. */
    private static Instant sp(int hora, int minuto) {
        return SEGUNDA.atTime(hora, minuto).atZone(SAO_PAULO).toInstant();
    }

    private static PeriodoOcupado ocupado(int horaInicio, int minInicio, int horaFim, int minFim) {
        return new PeriodoOcupado(sp(horaInicio, minInicio), sp(horaFim, minFim));
    }

    private List<Instant> calcular(List<IntervaloDiario> expediente, List<PeriodoOcupado> ocupados, int duracaoMin, int passoMin) {
        return DisponibilidadeCalculator.horariosLivres(SEGUNDA, SAO_PAULO, expediente, ocupados,
                Duration.ofMinutes(duracaoMin), passoMin, SEM_LIMITE_INFERIOR, SEM_LIMITE_SUPERIOR);
    }

    @Test
    void oferecaHorariosDaGradeDentroDoExpediente() {
        List<Instant> livres = calcular(List.of(MANHA), List.of(), 30, 30);

        assertThat(livres).containsExactly(sp(9, 0), sp(9, 30), sp(10, 0), sp(10, 30), sp(11, 0), sp(11, 30));
    }

    @Test
    void pausaEntreDoisIntervalosFicaSemHorarios() {
        List<Instant> livres = calcular(List.of(MANHA, TARDE), List.of(), 30, 30);

        assertThat(livres).hasSize(14).doesNotContain(sp(12, 0), sp(12, 30), sp(13, 0), sp(13, 30));
        assertThat(livres).contains(sp(11, 30), sp(14, 0), sp(17, 30));
    }

    @Test
    void atendimentoPrecisaCabeInteiroNoIntervaloEntaoNaoCruzaOFechamento() {
        // 90min: o ultimo inicio possivel na manha e 10:30 (termina 12:00); 11:00 terminaria 12:30
        List<Instant> livres = calcular(List.of(MANHA, TARDE), List.of(), 90, 30);

        assertThat(livres).containsExactly(
                sp(9, 0), sp(9, 30), sp(10, 0), sp(10, 30),
                sp(14, 0), sp(14, 30), sp(15, 0), sp(15, 30), sp(16, 0), sp(16, 30));
    }

    @Test
    void servicoMaiorQueOIntervaloNaoTemHorario() {
        List<Instant> livres = calcular(List.of(new IntervaloDiario(LocalTime.of(9, 0), LocalTime.of(9, 20))), List.of(), 30, 30);

        assertThat(livres).isEmpty();
    }

    @Test
    void semExpedienteNoDiaNaoHaHorarios() {
        assertThat(calcular(List.of(), List.of(), 30, 30)).isEmpty();
    }

    @Test
    void horarioOcupadoSaiDaListaMasOsVizinhosEncostadosContinuam() {
        // ocupado 10:00-10:30: o das 09:30 termina exatamente as 10:00 e o das 10:30 comeca exatamente as 10:30
        List<Instant> livres = calcular(List.of(MANHA), List.of(ocupado(10, 0, 10, 30)), 30, 30);

        assertThat(livres).containsExactly(sp(9, 0), sp(9, 30), sp(10, 30), sp(11, 0), sp(11, 30));
    }

    @Test
    void atendimentoLongoQueSobreporiaOOcupadoEDescartadoMasEncostarPode() {
        // 60min com ocupado 10:00-10:30: 09:00 (ate 10:00) encosta e vale; 09:30 e 10:00 invadem; 10:30 e 11:00 valem
        List<Instant> livres = calcular(List.of(MANHA), List.of(ocupado(10, 0, 10, 30)), 60, 30);

        assertThat(livres).containsExactly(sp(9, 0), sp(10, 30), sp(11, 0));
    }

    @Test
    void ocupadoQueComecouNoDiaAnteriorEAtravessaAMeiaNoiteTambemBloqueia() {
        PeriodoOcupado viradaDaNoite = new PeriodoOcupado(
                SEGUNDA.minusDays(1).atTime(22, 0).atZone(SAO_PAULO).toInstant(), sp(9, 30));

        List<Instant> livres = calcular(List.of(MANHA), List.of(viradaDaNoite), 30, 30);

        assertThat(livres).startsWith(sp(9, 30)).doesNotContain(sp(9, 0));
    }

    @Test
    void antecedenciaMinimaDescartaHorariosQueJaComecaramOuEstaoMuitoPerto() {
        Instant agoraMaisAntecedencia = sp(10, 20);

        List<Instant> livres = DisponibilidadeCalculator.horariosLivres(SEGUNDA, SAO_PAULO, List.of(MANHA), List.of(),
                Duration.ofMinutes(30), 30, agoraMaisAntecedencia, SEM_LIMITE_SUPERIOR);

        assertThat(livres).containsExactly(sp(10, 30), sp(11, 0), sp(11, 30));
    }

    @Test
    void limiteSuperiorEExclusivo() {
        List<Instant> livres = DisponibilidadeCalculator.horariosLivres(SEGUNDA, SAO_PAULO, List.of(MANHA), List.of(),
                Duration.ofMinutes(30), 30, SEM_LIMITE_INFERIOR, sp(11, 0));

        assertThat(livres).containsExactly(sp(9, 0), sp(9, 30), sp(10, 0), sp(10, 30));
    }

    @Test
    void gradeAlinhadaAoRelogioEnaoAoInicioDoExpediente() {
        IntervaloDiario comecaQuebrado = new IntervaloDiario(LocalTime.of(9, 10), LocalTime.of(11, 0));

        assertThat(calcular(List.of(comecaQuebrado), List.of(), 30, 30))
                .containsExactly(sp(9, 30), sp(10, 0), sp(10, 30));
        assertThat(calcular(List.of(comecaQuebrado), List.of(), 30, 15))
                .containsExactly(sp(9, 15), sp(9, 30), sp(9, 45), sp(10, 0), sp(10, 15), sp(10, 30));
    }

    @Test
    void resultadoSaiOrdenadoMesmoComExpedienteInformadoForaDeOrdem() {
        List<Instant> livres = calcular(List.of(TARDE, MANHA), List.of(), 120, 60);

        assertThat(livres).isSorted();
        assertThat(livres).first().isEqualTo(sp(9, 0));
    }

    @Test
    void respeitaOFusoDoNegocioAoConverterHoraDeParedeEmInstante() {
        ZoneId manaus = ZoneId.of("America/Manaus"); // UTC-4, sem horario de verao
        List<Instant> livres = DisponibilidadeCalculator.horariosLivres(SEGUNDA, manaus,
                List.of(new IntervaloDiario(LocalTime.of(9, 0), LocalTime.of(10, 0))), List.of(),
                Duration.ofMinutes(60), 30, SEM_LIMITE_INFERIOR, SEM_LIMITE_SUPERIOR);

        assertThat(livres).containsExactly(Instant.parse("2026-09-21T13:00:00Z"));
    }

    @Test
    void recusaDuracaoEPassoInvalidos() {
        assertThatThrownBy(() -> calcular(List.of(MANHA), List.of(), 0, 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calcular(List.of(MANHA), List.of(), 30, 0)).isInstanceOf(IllegalArgumentException.class);
    }

}
