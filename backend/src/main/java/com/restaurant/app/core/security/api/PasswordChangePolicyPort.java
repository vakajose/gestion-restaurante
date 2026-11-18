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

    /**
     * Indica si el correo electrónico es obligatorio al crear o editar colaboradores en el tenant.
     */
    default boolean isUserEmailRequired(UUID tenantId) {
        return true;
    }
}
