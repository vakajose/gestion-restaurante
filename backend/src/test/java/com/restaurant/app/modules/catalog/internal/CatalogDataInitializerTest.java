package com.restaurant.app.modules.catalog.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class CatalogDataInitializerTest {

    @Autowired
    private CatalogDataInitializer catalogDataInitializer;

    @Test
    @DisplayName("CatalogDataInitializer should run all seed routines without unique constraint violations")
    void shouldSeedCatalogsWithoutConstraintViolations() {
        assertDoesNotThrow(() -> catalogDataInitializer.run());
    }
}
