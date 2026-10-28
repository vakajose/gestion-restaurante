package com.restaurant.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class RestaurantModularityTests {

    @Test
    void verifyModularity() {
        ApplicationModules modules = ApplicationModules.of(RestaurantApplication.class);
        modules.verify();
    }

    @Test
    void businessModulesShouldNotDependOnCoreRepositories() {
        JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.restaurant.app");

        ArchRule rule = noClasses()
            .that().resideInAnyPackage(
                "com.restaurant.app.modules.pos..",
                "com.restaurant.app.modules.catalog..",
                "com.restaurant.app.modules.inventory..",
                "com.restaurant.app.modules.finance.."
            )
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.restaurant.app.core.common.repository.."
            )
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                "com.restaurant.app.core.security.AppUserRepository"
            )
            .because("Business modules must interact with core entities via public APIs (.api), never directly with core repositories");

        rule.check(productionClasses);
    }
}
