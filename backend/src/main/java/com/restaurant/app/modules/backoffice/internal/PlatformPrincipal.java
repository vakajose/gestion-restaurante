package com.restaurant.app.modules.backoffice.internal;

import java.util.UUID;

record PlatformPrincipal(
    UUID adminId,
    String username,
    String email,
    String fullName,
    PlatformRole role,
    boolean mustChangePassword
) {
}
