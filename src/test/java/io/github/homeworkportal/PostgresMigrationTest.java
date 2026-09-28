package io.github.homeworkportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PostgresMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.storage.directory", () -> "target/postgres-test-uploads");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void postgresMigrationCreatesPortalTables() {
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.tables " +
                "where table_schema='public' and table_name='user_accounts'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history " +
                "where version='1' and success=true", Integer.class));
    }
}
