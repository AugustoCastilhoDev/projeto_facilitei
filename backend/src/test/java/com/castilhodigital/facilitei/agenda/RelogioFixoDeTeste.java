package com.castilhodigital.facilitei.agenda;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fixa o "agora" dos testes da agenda no domingo 2026-09-20 12:00 (Sao Paulo), para os controllers - que leem
 * o Clock - calcularem antecedencia e horizonte sempre igual, seja qual for o dia em que a suite roda. O bean
 * tem outro NOME que o de producao (relogio x clock) so para nao colidir; @Primary o faz vencer.
 */
@TestConfiguration
class RelogioFixoDeTeste {

    static final Instant AGORA = LocalDate.of(2026, 9, 20).atTime(12, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();

    @Bean
    @Primary
    Clock relogioFixo() {
        return Clock.fixed(AGORA, ZoneId.of("UTC"));
    }

}
