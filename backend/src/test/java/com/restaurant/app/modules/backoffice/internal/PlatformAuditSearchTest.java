package com.restaurant.app.modules.backoffice.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAuditSearchTest {

    @Mock
    private PlatformAuditLogRepository auditLogRepository;

    private PlatformAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new PlatformAuditService(auditLogRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("search soporta filtro textual libre y paginación sobre bitácora inmutable")
    @SuppressWarnings("unchecked")
    void shouldSearchWithFreeTextAndPagination() {
        PlatformAuditLog entry = new PlatformAuditLog(
            UUID.randomUUID(),
            "superadmin",
            "TENANT_USER_PASSWORD_RESET",
            "APP_USER",
            "user-99",
            "{\"username\":\"cajero1\",\"mustChangePassword\":true}",
            "10.0.0.1"
        );
        ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(entry, "createdAt", Instant.now());

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenAnswer(inv -> {
                Pageable p = inv.getArgument(1);
                return new PageImpl<>(List.of(entry), p, 45);
            });

        Page<PlatformAuditService.AuditLogDto> page = auditService.search(
            null,
            null,
            "cajero1",
            null,
            null,
            1,
            20
        );

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isEqualTo(45);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().detailsJson()).contains("cajero1");
    }
}
