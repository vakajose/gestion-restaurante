package com.restaurant.app.core.entitlements.api;

import com.restaurant.app.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Excepción RFC 7807 (HTTP 403) para bloqueos de suscripción, operación POS/Caja y exceso de cuotas.
 */
public class EntitlementBlockedException extends DomainException {

    public static final String CODE_TENANT_SUSPENDED = "TENANT_SUSPENDED";
    public static final String CODE_OPERATIONS_SUSPENDED = "TENANT_OPERATIONS_SUSPENDED";
    public static final String CODE_QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String CODE_OVER_QUOTA_RESOLUTION_REQUIRED = "OVER_QUOTA_RESOLUTION_REQUIRED";

    private EntitlementBlockedException(String errorCode, String title, String message) {
        super(
            HttpStatus.FORBIDDEN,
            errorCode,
            message,
            URI.create("https://restaurant.com/errors/" + errorCode.toLowerCase().replace('_', '-')),
            title
        );
    }

    public static EntitlementBlockedException totalSuspension(String reason) {
        String detail = (reason != null && !reason.isBlank())
            ? "La cuenta de la empresa se encuentra suspendida (acceso total bloqueado): " + reason
            : "La cuenta de la empresa se encuentra suspendida por falta de suscripción vigente.";
        return new EntitlementBlockedException(CODE_TENANT_SUSPENDED, "Empresa suspendida", detail);
    }

    public static EntitlementBlockedException operationsSuspended(String reason) {
        String detail = (reason != null && !reason.isBlank())
            ? "La operación POS y apertura de turno están suspendidas (modo solo consulta): " + reason
            : "Tu suscripción requiere regularización. Puedes consultar información histórica, pero la apertura de turno y ventas POS están bloqueadas.";
        return new EntitlementBlockedException(CODE_OPERATIONS_SUSPENDED, "Operación POS suspendida", detail);
    }

    public static EntitlementBlockedException quotaExceeded(QuotaResource resource, long attemptedTotal, int limit) {
        return new EntitlementBlockedException(
            CODE_QUOTA_EXCEEDED,
            "Límite del plan alcanzado",
            String.format(
                "Se alcanzó el límite de tu plan para '%s' (intento: %d, límite configurado: %d). Actualiza tu plan o desactiva registros inactivos/excedentes.",
                resource.displayName(), attemptedTotal, limit
            )
        );
    }

    public static EntitlementBlockedException overQuotaResolutionRequired(List<QuotaResource> excessResources) {
        String resourcesText = excessResources.stream()
            .map(QuotaResource::displayName)
            .collect(Collectors.joining(", "));
        return new EntitlementBlockedException(
            CODE_OVER_QUOTA_RESOLUTION_REQUIRED,
            "Regularización de cuota requerida tras cambio de plan",
            "Tus recursos activos superan los límites del plan actual (" + resourcesText
                + "). Desactiva manualmente los excedentes en Administración antes de abrir turno o vender en POS."
        );
    }
}
