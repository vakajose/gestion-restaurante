package com.restaurant.app.core.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Explicit Flyway migration configuration that enforces execution before
 * JPA EntityManagerFactory initialization and provides self-healing against
 * empty baseline states.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Enforces that EntityManagerFactory depends on the "flyway" bean,
     * ensuring migrations are fully executed before Hibernate schema validation.
     */
    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor flywayEntityManagerFactoryDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("flyway");
    }

    @Bean
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:0}") String baselineVersion
    ) {
        log.info("Starting Flyway database migration check (locations: {}, baselineVersion: {})...",
                locations, baselineVersion);

        // Self-healing: if flyway_schema_history exists but app_users is missing,
        // an empty baseline occurred previously. Reset history so V1 can execute.
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                boolean appUsersExists = false;
                try (ResultSet rs = stmt.executeQuery("SELECT to_regclass('public.app_users')")) {
                    if (rs.next() && rs.getString(1) != null) {
                        appUsersExists = true;
                    }
                }

                if (!appUsersExists) {
                    boolean historyExists = false;
                    try (ResultSet rs = stmt.executeQuery("SELECT to_regclass('public.flyway_schema_history')")) {
                        if (rs.next() && rs.getString(1) != null) {
                            historyExists = true;
                        }
                    }

                    if (historyExists) {
                        log.warn("flyway_schema_history exists but app_users is missing. Cleaning stale baseline to apply V1...");
                        stmt.execute("DROP TABLE IF EXISTS flyway_schema_history CASCADE");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Non-critical schema pre-check notice: {}", e.getMessage());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();

        var result = flyway.migrate();
        log.info("Flyway migration completed successfully. Applied {} migrations.", result.migrationsExecuted);
        return flyway;
    }
}
