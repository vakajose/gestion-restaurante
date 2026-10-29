package com.restaurant.app.modules.finance.api;

import java.util.Optional;
import java.util.UUID;

public interface FinancePublicApi {

    /**
     * Retorna el turno de caja actualmente abierto para una sucursal, si existe.
     */
    Optional<CashShiftDto> findActiveShift(UUID tenantId, UUID branchId);

    /**
     * Indica si existe un turno de caja abierto en la sucursal.
     */
    boolean hasActiveShift(UUID tenantId, UUID branchId);

    /**
     * Registra las ventas del cobro de una comanda en el turno de caja activo.
     */
    void recordSale(UUID tenantId, UUID branchId, UUID cashShiftId, String paymentMethod, java.math.BigDecimal amount);
}
