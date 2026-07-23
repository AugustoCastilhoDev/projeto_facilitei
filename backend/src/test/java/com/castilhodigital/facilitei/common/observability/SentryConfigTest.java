package com.castilhodigital.facilitei.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Sentry;
import org.junit.jupiter.api.Test;

class SentryConfigTest {

    @Test
    void naoInicializaSentryQuandoDsnEmBranco() {
        SentryConfig config = new SentryConfig(new SentryProperties("", "test"));

        config.inicializar();

        assertThat(Sentry.isEnabled()).isFalse();
    }

    @Test
    void naoInicializaSentryQuandoDsnNulo() {
        SentryConfig config = new SentryConfig(new SentryProperties(null, "test"));

        config.inicializar();

        assertThat(Sentry.isEnabled()).isFalse();
    }

}
