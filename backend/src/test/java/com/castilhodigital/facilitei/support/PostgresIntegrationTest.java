package com.castilhodigital.facilitei.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integracao: sobe a aplicacao inteira (Flyway +
 * Hibernate ddl-auto=validate + repositorios reais) contra um Postgres de
 * verdade. Os testes com Mockito/@WebMvcTest do resto da suite nao pegam
 * problemas de constraint, JPQL nem de transacao - foi assim que os bugs de
 * LazyInitializationException e de reserva de slot liberado passaram despercebidos.
 *
 * O container e unico por JVM (iniciado uma vez e reaproveitado por todas as
 * classes de teste; o Ryuk do Testcontainers o remove ao final) - subir um
 * por classe deixaria a suite lenta demais. O isolamento entre testes vem de
 * limpar as tabelas antes de cada um, nao de um banco novo.
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propriedadesDoBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // jobs agendados com o relogio real poderiam mexer nos dados de um teste no meio dele
        registry.add("facilitei.scheduling.enabled", () -> "false");
        // os testes fazem dezenas de reservas do mesmo IP (127.0.0.1); o limite por IP tem teste proprio
        registry.add("facilitei.agenda.limite-reservas-por-ip", () -> "100000");
        registry.add("facilitei.agenda.limite-avisos-de-pagamento-por-ip", () -> "100000");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        // CASCADE alcanca todas as tabelas que referenciam tenants (users, services,
        // slots, bookings, faturas...); as duas restantes nao tem FK para tenants.
        jdbcTemplate.execute("TRUNCATE TABLE tenants, clientes, codigos_verificacao RESTART IDENTITY CASCADE");
    }

}
