package com.restaurant.app.core.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DateTimeSerializationTests {

    @Autowired
    private ObjectMapper objectMapper;

    record DateTimeSample(LocalDate date, Instant timestamp) {}

    @Test
    void shouldSerializeDatesAccordingToAdr0006() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 30);
        Instant timestamp = Instant.parse("2026-09-30T14:30:00Z");
        DateTimeSample sample = new DateTimeSample(date, timestamp);

        String json = objectMapper.writeValueAsString(sample);

        assertThat(json).contains("\"date\":\"2026-09-30\"");
        assertThat(json).contains("\"timestamp\":\"2026-09-30T14:30:00Z\"");
    }
}
