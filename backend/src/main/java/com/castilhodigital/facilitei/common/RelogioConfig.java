package com.castilhodigital.facilitei.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O relogio como dependencia injetavel. Quem precisa do "agora" (controllers,
 * jobs) recebe um Clock em vez de chamar Instant.now() - assim os testes fixam
 * a data e nao quebram so porque o calendario andou (uma agenda de teste em
 * 2026-09-21 viraria "passado" e sem horarios livres).
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
