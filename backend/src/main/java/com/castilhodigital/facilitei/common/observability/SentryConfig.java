package com.castilhodigital.facilitei.common.observability;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Inicializacao manual do SDK core do Sentry (nao o starter oficial do
 * Spring Boot - ver pom.xml para o motivo). Sem DSN configurado, o SDK
 * simplesmente nao inicializa e Sentry.captureException(...) vira um no-op
 * seguro em qualquer lugar do codigo que o chame.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SentryProperties.class)
@RequiredArgsConstructor
public class SentryConfig {

    private final SentryProperties sentryProperties;

    @PostConstruct
    public void inicializar() {
        if (!StringUtils.hasText(sentryProperties.dsn())) {
            log.info("Sentry desativado (facilitei.sentry.dsn em branco) - erros nao serao enviados.");
            return;
        }
        Sentry.init(options -> {
            options.setDsn(sentryProperties.dsn());
            options.setEnvironment(sentryProperties.environment());
        });
        log.info("Sentry ativado (environment={}).", sentryProperties.environment());
    }

}
