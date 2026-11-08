package com.restaurant.app.modules.finance.api;

import java.util.UUID;

public record ValidatePinResponse(
    boolean valid,
    UUID supervisorId,
    String supervisorName
) {}
