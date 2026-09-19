package com.castilhodigital.facilitei.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JanelaDeLimiteTest {

    private static final Duration DEZ_MINUTOS = Duration.ofMinutes(10);

    /** Relogio que o teste move a mao: e o que prova o comportamento da janela sem esperar de verdade. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-09-20T15:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();

    @Test
    void deixaPassarAteOMaximoERecusaDaiEmDiante() {
        JanelaDeLimite limite = new JanelaDeLimite(3, DEZ_MINUTOS, relogio);

        assertThat(limite.permitir("1.1.1.1")).isTrue();
        assertThat(limite.permitir("1.1.1.1")).isTrue();
        assertThat(limite.permitir("1.1.1.1")).isTrue();
        assertThat(limite.permitir("1.1.1.1")).as("4a tentativa").isFalse();
        assertThat(limite.permitir("1.1.1.1")).as("continua recusando na mesma janela").isFalse();
    }

    @Test
    void cadaIpTemSuaPropriaContagem() {
        JanelaDeLimite limite = new JanelaDeLimite(1, DEZ_MINUTOS, relogio);

        assertThat(limite.permitir("1.1.1.1")).isTrue();
        assertThat(limite.permitir("1.1.1.1")).isFalse();
        assertThat(limite.permitir("2.2.2.2")).as("outro IP nao paga pelo primeiro").isTrue();
    }

    @Test
    void depoisDeAJanelaFecharOIpVoltaAPoderTentar() {
        JanelaDeLimite limite = new JanelaDeLimite(2, DEZ_MINUTOS, relogio);
        limite.permitir("1.1.1.1");
        limite.permitir("1.1.1.1");
        assertThat(limite.permitir("1.1.1.1")).isFalse();

        relogio.avancar(DEZ_MINUTOS.minusSeconds(1));
        assertThat(limite.permitir("1.1.1.1")).as("faltando 1s para a janela fechar").isFalse();

        relogio.avancar(Duration.ofSeconds(1));
        assertThat(limite.permitir("1.1.1.1")).as("janela fechou: recomeca do zero").isTrue();
        assertThat(limite.permitir("1.1.1.1")).isTrue();
        assertThat(limite.permitir("1.1.1.1")).isFalse();
    }

    /** Insistir depois de recusado nao adia a liberacao: a janela nao "desliza" com quem insiste. */
    @Test
    void insistirNaoPrologaOBloqueio() {
        JanelaDeLimite limite = new JanelaDeLimite(1, DEZ_MINUTOS, relogio);
        limite.permitir("1.1.1.1");

        relogio.avancar(Duration.ofMinutes(9));
        assertThat(limite.permitir("1.1.1.1")).isFalse();
        relogio.avancar(Duration.ofMinutes(1));

        assertThat(limite.permitir("1.1.1.1")).isTrue();
    }

    @Test
    void limparRemoveSoAsJanelasFechadas() {
        JanelaDeLimite limite = new JanelaDeLimite(1, DEZ_MINUTOS, relogio);
        limite.permitir("velho");
        relogio.avancar(Duration.ofMinutes(6));
        limite.permitir("novo");
        relogio.avancar(Duration.ofMinutes(5));   // "velho" tem 11 min (fechada), "novo" tem 5 (aberta)

        limite.limparExpirados();

        assertThat(limite.chavesGuardadas()).isEqualTo(1);
        assertThat(limite.permitir("novo")).as("a contagem do 'novo' foi preservada: a 2a ocorrencia passa do teto de 1").isFalse();
        assertThat(limite.permitir("velho")).as("o 'velho' recomecou do zero").isTrue();
    }

}
