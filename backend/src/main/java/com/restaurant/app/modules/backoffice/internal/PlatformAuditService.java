package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class PlatformAuditService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAuditService.class);

    private final PlatformAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    PlatformAuditService(PlatformAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    record AuditLogDto(
        UUID id,
        UUID actorAdminId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String detailsJson,
        String ipAddress,
        Instant createdAt
    ) {
        static AuditLogDto fromEntity(PlatformAuditLog entity) {
            return new AuditLogDto(
                entity.getId(),
                entity.getActorAdminId(),
                entity.getActorUsername(),
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getDetailsJson(),
                entity.getIpAddress(),
                entity.getCreatedAt()
            );
        }
    }

    @Transactional
    public void record(
        PlatformPrincipal actor,
        String action,
        String targetType,
        String targetId,
        Map<String, ?> details,
        String ipAddress
    ) {
        UUID actorId = actor != null ? actor.adminId() : null;
        String actorUsername = actor != null ? actor.username() : "SYSTEM";
        saveEntry(actorId, actorUsername, action, targetType, targetId, details, ipAddress);
    }

    @Transactional
    public void recordSystem(
        String action,
        String targetType,
        String targetId,
        Map<String, ?> details
    ) {
        saveEntry(null, "SYSTEM", action, targetType, targetId, details, "127.0.0.1");
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(
        String actor,
        String action,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<PlatformAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                String pattern = "%" + actor.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("actorUsername")), pattern));
            }
            if (action != null && !action.isBlank()) {
                String pattern = "%" + action.trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("action")), pattern));
            }
            if (from != null) {
                Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (to != null) {
                Instant endExclusive = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.lessThan(root.get("createdAt"), endExclusive));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return auditLogRepository.findAll(spec, pageable).map(AuditLogDto::fromEntity);
    }

    static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private void saveEntry(
        UUID actorId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        Map<String, ?> details,
        String ipAddress
    ) {
        String serialized = null;
        if (details != null && !details.isEmpty()) {
            try {
                serialized = objectMapper.writeValueAsString(details);
            } catch (Exception ex) {
                log.warn("No se pudo serializar detailsJson para acción {}: {}", action, ex.getMessage());
                serialized = details.toString();
            }
        }
        auditLogRepository.save(new PlatformAuditLog(
            actorId,
            actorUsername,
            action,
            targetType,
            targetId,
            serialized,
            ipAddress
        ));
    }
}
