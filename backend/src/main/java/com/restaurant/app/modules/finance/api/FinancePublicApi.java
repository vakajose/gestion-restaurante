package com.restaurant.app.modules.finance.api;

import java.util.Optional;
import java.util.UUID;

public interface FinancePublicApi {

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
}
