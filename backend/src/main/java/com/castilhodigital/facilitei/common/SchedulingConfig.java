package com.castilhodigital.facilitei.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga os jobs agendados (@Scheduled). Pode ser desligado com
 * facilitei.scheduling.enabled=false - os testes de integracao fazem isso: um job
 * rodando por baixo, com o relogio real, poderia expirar ou faturar os dados de um
 * teste no meio dele. Em producao nao se define a propriedade (ligado por padrao).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "facilitei.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
