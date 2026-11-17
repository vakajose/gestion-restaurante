package com.restaurant.app.core.security.api;

import java.util.UUID;

/**
 * Puerto de lectura de política de cambio obligatorio de contraseña por empresa,
 * evitando dependencias circulares entre core.security y modules.configuration.
 */
public interface PasswordChangePolicyPort {

    /**
     * Indica si el tenant exige el cambio de contraseña temporal cuando user.mustChangePassword=true.
     */
    boolean isPasswordChangeEnforcedOnReset(UUID tenantId);
}
