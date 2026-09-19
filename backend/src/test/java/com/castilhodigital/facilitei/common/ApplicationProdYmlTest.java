package com.castilhodigital.facilitei.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/**
 * Garante que o application-prod.yml continua SEM valor default para os
 * segredos: se alguem "facilitar" trocando ${JWT_SECRET} por
 * ${JWT_SECRET:algum-valor}, a aplicacao passaria a subir em producao com um
 * segredo conhecido sem ninguem perceber. O ambiente aqui e isolado (nao le
 * variaveis de ambiente da maquina), entao o resultado nao depende de onde o
 * teste roda.
 */
class ApplicationProdYmlTest {

    private MockEnvironment ambiente;

    @BeforeEach
    void carregarYmlDeProducao() throws IOException {
        ambiente = new MockEnvironment();
        for (PropertySource<?> fonte : new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"))) {
            ambiente.getPropertySources().addLast(fonte);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "facilitei.jwt.secret, JWT_SECRET",
            "facilitei.security.credentials-encryption-key, CREDENTIALS_ENCRYPTION_KEY",
            "facilitei.security.credentials-encryption-salt, CREDENTIALS_ENCRYPTION_SALT",
            "facilitei.notification.provider, NOTIFICATION_PROVIDER",
            "facilitei.frontend.base-url, FRONTEND_BASE_URL"})
    void naoSobeSemAVariavelDeAmbienteObrigatoria(String propriedade, String variavel) {
        assertThatThrownBy(() -> ambiente.getRequiredProperty(propriedade))
                .hasMessageContaining(variavel);

        ambiente.setProperty(variavel, "valor-definido-no-deploy");

        assertThat(ambiente.getRequiredProperty(propriedade)).isEqualTo("valor-definido-no-deploy");
    }

    @Test
    void configuraOProxyReversoELogsDeProducao() {
        assertThat(ambiente.getProperty("server.forward-headers-strategy")).isEqualTo("native");
        assertThat(ambiente.getProperty("logging.level.com.castilhodigital.facilitei")).isEqualTo("INFO");
        assertThat(ambiente.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(ambiente.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
    }

}
