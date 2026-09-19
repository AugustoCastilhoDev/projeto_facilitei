package com.castilhodigital.facilitei.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Teste de fumaca da infraestrutura: se o contexto sobe, o Flyway aplicou
 * todas as migrations num Postgres limpo E o Hibernate (ddl-auto=validate)
 * confirmou que cada entidade bate com o schema resultante.
 */
class AplicacaoSobeComPostgresRealIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAplicaTodasAsMigrationsEOHibernateValidaOSchema() {
        Integer migrationsAplicadas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);

        assertThat(migrationsAplicadas).isGreaterThanOrEqualTo(15);
    }

}
