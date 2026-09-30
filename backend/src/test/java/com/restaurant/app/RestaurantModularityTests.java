package com.restaurant.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class RestaurantModularityTests {

    @Test
    void verifyModularity() {
        ApplicationModules modules = ApplicationModules.of(RestaurantApplication.class);
        modules.verify();
    }
}
