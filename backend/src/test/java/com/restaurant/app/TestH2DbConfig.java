package com.restaurant.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@Order(-100)
public class TestH2DbConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestH2DbConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public TestH2DbConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE event_publication ALTER COLUMN serialized_event SET DATA TYPE CHARACTER VARYING(65535)");
            log.info("H2: event_publication.serialized_event column enlarged to 65535 chars.");
        } catch (Exception e) {
            log.warn("Could not alter event_publication: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE event_publication_archive ALTER COLUMN serialized_event SET DATA TYPE CHARACTER VARYING(65535)");
            log.info("H2: event_publication_archive.serialized_event column enlarged to 65535 chars.");
        } catch (Exception e) {
            log.warn("Could not alter event_publication_archive: {}", e.getMessage());
        }
    }
}
