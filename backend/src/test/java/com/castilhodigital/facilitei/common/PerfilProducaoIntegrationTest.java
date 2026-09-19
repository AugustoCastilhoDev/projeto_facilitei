package com.castilhodigital.facilitei.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.castilhodigital.facilitei.support.PostgresIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Sobe a aplicacao inteira com o perfil "prod" numa porta real, do jeito que
 * ela roda atras do proxy reverso: o cliente aqui e o proprio "proxy" (loopback,
 * uma origem interna confiavel) informando o IP real do usuario em
 * X-Forwarded-For.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "JWT_SECRET=k8Jd93hFq0LzXv27aPwR5mTnB6yUcE41",
        "CREDENTIALS_ENCRYPTION_KEY=Zq81LmXc0vNr47TbHy5WdKs92AeJfP63",
        "CREDENTIALS_ENCRYPTION_SALT=a1b2c3d4e5f60718",
        "NOTIFICATION_PROVIDER=myzap",
        "FRONTEND_BASE_URL=https://app.barbearia.com.br",
        "MYZAP_API_KEY=chave-de-teste"})
class PerfilProducaoIntegrationTest extends PostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private ApplicationContext contexto;

    private int tentarLogin(String ipDoCliente) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ipDoCliente)
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"ninguem@example.com\",\"senha\":\"senha-errada-123\"}"))
                .build();
        return http.send(requisicao, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void validadorDeProducaoEstaAtivoNoPerfilProd() {
        assertThat(contexto.getBeansOfType(ConfiguracaoProducaoValidator.class)).hasSize(1);
    }

    @Test
    void rateLimitDoLoginValePorClienteRealAtrasDoProxyEDeNaoPeloIpDoProxy() throws Exception {
        String atacante = "203.0.113.10";
        String outroUsuario = "203.0.113.99";

        for (int i = 0; i < 5; i++) {
            assertThat(tentarLogin(atacante)).as("tentativa %d com senha errada", i + 1).isEqualTo(401);
        }
        assertThat(tentarLogin(atacante)).as("6a tentativa do mesmo cliente").isEqualTo(429);

        // sem forward-headers, os dois clientes seriam "127.0.0.1" e este tambem levaria 429
        assertThat(tentarLogin(outroUsuario)).as("outro cliente atras do mesmo proxy").isEqualTo(401);
    }

}
