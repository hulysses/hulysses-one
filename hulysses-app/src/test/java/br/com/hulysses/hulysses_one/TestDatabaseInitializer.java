package br.com.hulysses.hulysses_one;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (!Boolean.getBoolean("hulysses.test.postgresql")) {
            return;
        }
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/hulysses_one");
        String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        String password = System.getenv("DB_PASSWORD");
        if (password == null) {
            throw new IllegalStateException("Set DB_PASSWORD to run isolated PostgreSQL tests");
        }
        String schema = "etapa1_test_" + UUID.randomUUID().toString().replace("-", "");
        execute(url, username, password, "create schema " + schema);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedPostgres", Map.of(
                "spring.datasource.url", url,
                "spring.datasource.username", username,
                "spring.datasource.password", password,
                "spring.datasource.driver-class-name", "org.postgresql.Driver",
                "spring.datasource.hikari.schema", schema,
                "spring.jpa.properties.hibernate.default_schema", schema,
                "spring.jpa.hibernate.ddl-auto", "create")));
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                execute(url, username, password, "drop schema " + schema + " cascade");
            }
        });
    }

    private void execute(String url, String username, String password, String sql) {
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not prepare or clean isolated PostgreSQL test schema", exception);
        }
    }
}
