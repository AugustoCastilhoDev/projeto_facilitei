package com.castilhodigital.facilitei.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.crypto.CredentialsEncryptionProperties;
import org.junit.jupiter.api.Test;

class ConfiguracaoProducaoValidatorTest {

    private static final String JWT_OK = "k8Jd93hFq0LzXv27aPwR5mTnB6yUcE41";
    private static final String CHAVE_OK = "Zq81LmXc0vNr47TbHy5WdKs92AeJfP63";
    private static final String SALT_OK = "a1b2c3d4e5f60718";
    private static final String FRONTEND_OK = "https://app.barbearia.com.br";

    private ConfiguracaoProducaoValidator validador(String jwt, String chave, String salt, String provider) {
        return validador(jwt, chave, salt, provider, FRONTEND_OK);
    }

    private ConfiguracaoProducaoValidator validador(String jwt, String chave, String salt, String provider, String frontend) {
        return new ConfiguracaoProducaoValidator(jwt, provider, frontend, new CredentialsEncryptionProperties(chave, salt));
    }

    @Test
    void aceitaConfiguracaoComSegredosProprios() {
        assertThatCode(() -> validador(JWT_OK, CHAVE_OK, SALT_OK, "myzap").validar()).doesNotThrowAnyException();
    }

    @Test
    void recusaOJwtSecretDeDesenvolvimentoDoRepositorio() {
        String segredoDoRepositorio = "facilitei-local-dev-secret-troque-em-producao-0123456789";

        assertThatThrownBy(() -> validador(segredoDoRepositorio, CHAVE_OK, SALT_OK, "myzap").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("desenvolvimento");
    }

    @Test
    void recusaAChaveDeCifraDeDesenvolvimentoDoRepositorio() {
        String chaveDoRepositorio = "facilitei-local-dev-encryption-key-troque-em-producao";

        assertThatThrownBy(() -> validador(JWT_OK, chaveDoRepositorio, SALT_OK, "myzap").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CREDENTIALS_ENCRYPTION_KEY");
    }

    @Test
    void recusaOSaltDeDesenvolvimentoDoRepositorio() {
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, "deadbeefcafe0123", "myzap").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CREDENTIALS_ENCRYPTION_SALT");
    }

    @Test
    void recusaSegredoCurtoDemais() {
        assertThatThrownBy(() -> validador("curto", CHAVE_OK, SALT_OK, "myzap").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("32 caracteres");
    }

    @Test
    void recusaSaltQueNaoEHexadecimal() {
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, "isto-nao-e-hexadecimal", "myzap").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hexadecimal");
    }

    @Test
    void recusaProviderConsoleQueVazariaOsCodigosOtpNosLogs() {
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, SALT_OK, "console").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOTIFICATION_PROVIDER");
    }

    /** Em http o token do link do dono trafegaria em texto puro. */
    @Test
    void recusaFrontendEmHttp() {
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, SALT_OK, "myzap", "http://app.barbearia.com.br").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_BASE_URL")
                .hasMessageContaining("https");
    }

    /** O dono receberia, no WhatsApp, um link que so abre na maquina de quem desenvolve. */
    @Test
    void recusaFrontendApontandoParaLocalhostOuVazio() {
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, SALT_OK, "myzap", "https://localhost:4200").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_BASE_URL")
                .hasMessageContaining("localhost");
        assertThatThrownBy(() -> validador(JWT_OK, CHAVE_OK, SALT_OK, "myzap", "").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_BASE_URL");
    }

    @Test
    void relataTodosOsProblemasDeUmaVezEmVezDeUmPorDeploy() {
        assertThatThrownBy(() -> validador("curto", "curta", "xyz", "console", "http://x").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("CREDENTIALS_ENCRYPTION_KEY")
                .hasMessageContaining("CREDENTIALS_ENCRYPTION_SALT")
                .hasMessageContaining("NOTIFICATION_PROVIDER")
                .hasMessageContaining("FRONTEND_BASE_URL");
    }

}
