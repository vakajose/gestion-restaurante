package com.restaurant.app.modules.finance.api;

import java.util.Optional;
import java.util.UUID;

public interface FinancePublicApi {

    /**
     * Abre un nuevo turno de caja en la sucursal (ADR-0013 / PB-034).
     */
    CashShiftDto openShift(OpenShiftRequest request);

    /**
     * Retorna el turno de caja actualmente abierto para una sucursal, si existe.
     * En modo SINGLE_PER_BRANCH retorna el turno de la sucursal.
     * En modo INDIVIDUAL_PER_CASHIER busca el turno del usuario autenticado si existe.
     */
    Optional<CashShiftDto> findActiveShift(UUID tenantId, UUID branchId);

    /**
     * Retorna el turno de caja abierto específicamente para un usuario en una sucursal.
     */
    Optional<CashShiftDto> findActiveShiftForUser(UUID tenantId, UUID branchId, UUID userId);

    /**
     * Lista todos los turnos abiertos concurrentes en una sucursal.
     */
    java.util.List<CashShiftDto> listActiveShifts(UUID tenantId, UUID branchId);

    /**
     * Indica si existe un turno de caja abierto en la sucursal.
     */
    boolean hasActiveShift(UUID tenantId, UUID branchId);

    /**
     * Registra las ventas del cobro de una comanda en el turno de caja activo.
     */
    void recordSale(UUID tenantId, UUID branchId, UUID cashShiftId, String paymentMethod, java.math.BigDecimal amount);

    /**
     * Registra el reintegro de efectivo de una comanda cancelada en el turno activo (ADR-0019 / PB-031).
     */
    void recordRefund(UUID tenantId, UUID branchId, UUID cashShiftId, java.math.BigDecimal amount);

    /**
     * Valida el PIN numérico de un supervisor para operaciones in-situ.
     */
    ValidatePinResponse validateSupervisorPin(UUID tenantId, UUID branchId, String pin);

    /**
     * Crea una solicitud de autorización de turno (Cancelación o Gasto).
     */
    ShiftAuthorizationDto createAuthorizationRequest(UUID tenantId, UUID userId, CreateShiftAuthorizationRequest request);

    /**
     * Aprueba una autorización de turno.
     */
    ShiftAuthorizationDto approveAuthorization(UUID tenantId, UUID supervisorId, UUID authorizationId, String notes, String decisionChannel);

    /**
     * Obtiene una autorización de turno por ID.
     */
    Optional<ShiftAuthorizationDto> getAuthorization(UUID tenantId, UUID authorizationId);
}
