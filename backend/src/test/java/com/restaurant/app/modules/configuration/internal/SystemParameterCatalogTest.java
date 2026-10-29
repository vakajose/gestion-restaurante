package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterCriticality;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.ParameterType;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemParameterCatalogTest {

    @Test
    @DisplayName("El catálogo contiene todos los parámetros canónicos de SystemParameter")
    void shouldContainAllCanonicalParameters() {
        for (SystemParameter param : SystemParameter.values()) {
            Optional<SystemParameterCatalog.ParameterDefinition> def = SystemParameterCatalog.getDefinition(param.getKey());
            assertThat(def).isPresent();
            assertThat(def.get().key()).isEqualTo(param.getKey());
            assertThat(def.get().defaultValue()).isNotNull();
            assertThat(def.get().category()).isNotNull();
            assertThat(def.get().type()).isNotNull();
            assertThat(def.get().allowedScope()).isNotNull();
        }
    }

    @Test
    @DisplayName("Filtrado por categoría retorna únicamente parámetros correspondientes")
    void shouldFilterByCategory() {
        List<SystemParameterCatalog.ParameterDefinition> posParams = SystemParameterCatalog.getDefinitionsByCategory(ParameterCategory.POS);
        assertThat(posParams).isNotEmpty();
        assertThat(posParams).allMatch(p -> p.category() == ParameterCategory.POS);

        List<SystemParameterCatalog.ParameterDefinition> invParams = SystemParameterCatalog.getDefinitionsByCategory(ParameterCategory.INVENTORY);
        assertThat(invParams).isNotEmpty();
        assertThat(invParams).allMatch(p -> p.category() == ParameterCategory.INVENTORY);
    }

    @Test
    @DisplayName("inventory.deduction_mode está catalogado como HIGH criticality y solo nivel TENANT")
    void verifyInventoryDeductionModeMetadata() {
        SystemParameterCatalog.ParameterDefinition def = SystemParameterCatalog.getDefinition(SystemParameter.INVENTORY_DEDUCTION_MODE.getKey()).orElseThrow();
        assertThat(def.criticality()).isEqualTo(ParameterCriticality.HIGH);
        assertThat(def.allowedScope()).isEqualTo(ParameterScope.TENANT);
        assertThat(def.type()).isEqualTo(ParameterType.ENUM);
        assertThat(def.options()).contains("MANUAL_ISOLATED", "AUTOMATIC_PERMISSIVE", "AUTOMATIC_STRICT");
    }

    @Test
    @DisplayName("Validación rechaza valores vacíos o nulos")
    void validateRejectsEmptyValues() {
        SystemParameterCatalog.ParameterDefinition def = SystemParameterCatalog.getDefinition(SystemParameter.POS_DISPLAY_DISH_IMAGES.getKey()).orElseThrow();
        assertThatThrownBy(() -> SystemParameterCatalog.validate(def, ParameterScope.BRANCH, null))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> SystemParameterCatalog.validate(def, ParameterScope.BRANCH, "   "))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Validación de tipos: BOOLEAN, INTEGER, DOUBLE, ENUM")
    void validateTypes() {
        SystemParameterCatalog.ParameterDefinition boolDef = SystemParameterCatalog.getDefinition(SystemParameter.POS_DISPLAY_DISH_IMAGES.getKey()).orElseThrow();
        SystemParameterCatalog.validate(boolDef, ParameterScope.BRANCH, "true");
        SystemParameterCatalog.validate(boolDef, ParameterScope.BRANCH, "false");
        assertThatThrownBy(() -> SystemParameterCatalog.validate(boolDef, ParameterScope.BRANCH, "not_a_bool"))
            .isInstanceOf(ValidationException.class);

        SystemParameterCatalog.ParameterDefinition intDef = SystemParameterCatalog.getDefinition(SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES.getKey()).orElseThrow();
        SystemParameterCatalog.validate(intDef, ParameterScope.TENANT, "30");
        assertThatThrownBy(() -> SystemParameterCatalog.validate(intDef, ParameterScope.TENANT, "abc"))
            .isInstanceOf(ValidationException.class);

        SystemParameterCatalog.ParameterDefinition doubleDef = SystemParameterCatalog.getDefinition(SystemParameter.FINANCE_MAX_PETTY_CASH_EXPENSE.getKey()).orElseThrow();
        SystemParameterCatalog.validate(doubleDef, ParameterScope.BRANCH, "250.50");
        assertThatThrownBy(() -> SystemParameterCatalog.validate(doubleDef, ParameterScope.BRANCH, "invalid_num"))
            .isInstanceOf(ValidationException.class);

        SystemParameterCatalog.ParameterDefinition enumDef = SystemParameterCatalog.getDefinition(SystemParameter.INVENTORY_DEDUCTION_MODE.getKey()).orElseThrow();
        SystemParameterCatalog.validate(enumDef, ParameterScope.TENANT, "AUTOMATIC_STRICT");
        assertThatThrownBy(() -> SystemParameterCatalog.validate(enumDef, ParameterScope.TENANT, "UNKNOWN_MODE"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Validación de jerarquía de ámbito: BRANCH no puede sobreescribir parámetro exclusivo de TENANT")
    void validateScopeHierarchy() {
        SystemParameterCatalog.ParameterDefinition tenantOnlyDef = SystemParameterCatalog.getDefinition(SystemParameter.INVENTORY_DEDUCTION_MODE.getKey()).orElseThrow();
        assertThatThrownBy(() -> SystemParameterCatalog.validate(tenantOnlyDef, ParameterScope.BRANCH, "AUTOMATIC_STRICT"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no permite sobreescrituras a nivel de sucursal");
    }
}
