package com.restaurant.app;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Flyway should discover and recognize V1__init_schema.sql")
    void flywayShouldRecognizeInitialMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        MigrationInfo[] migrations = flyway.info().all();
        assertThat(migrations).isNotEmpty();

        MigrationInfo v1 = Arrays.stream(migrations)
                .filter(m -> "1".equals(m.getVersion().getVersion()))
                .findFirst()
                .orElse(null);

        assertThat(v1).isNotNull();
        assertThat(v1.getScript()).isEqualTo("V1__init_schema.sql");
        assertThat(v1.getDescription()).isEqualTo("init schema");
        assertThat(v1.getChecksum()).isNotNull();

        MigrationInfo v2 = Arrays.stream(migrations)
                .filter(m -> "2".equals(m.getVersion().getVersion()))
                .findFirst()
                .orElse(null);

        assertThat(v2).isNotNull();
        assertThat(v2.getScript()).isEqualTo("V2__spring_modulith_events.sql");
        assertThat(v2.getDescription()).isEqualTo("spring modulith events");
        assertThat(v2.getChecksum()).isNotNull();
    }


    @Test
    @DisplayName("V1__init_schema.sql should contain all required multi-tenant tables and composite indexes")
    void v1ScriptShouldContainAllRequiredTablesAndIndexes() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__init_schema.sql");
        assertThat(resource.exists()).isTrue();

        String sql;
        try (InputStream is = resource.getInputStream()) {
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 1. Verify all 18 tables defined in docs/sdd/04_database_schema.md
        List<String> requiredTables = List.of(
                "tenants",
                "tenant_settings",
                "branches",
                "branch_settings",
                "app_users",
                "user_preferences",
                "categories",
                "ingredients",
                "dishes",
                "dish_recipes",
                "branch_dishes",
                "branch_stock",
                "purchases",
                "purchase_items",
                "kardex_movements",
                "orders",
                "order_items",
                "cash_shifts",
                "expenses",
                "audit_logs"
        );

        for (String table : requiredTables) {
            assertThat(sql).containsIgnoringCase("CREATE TABLE " + table);
        }

        // 2. Verify all high-performance composite indexes
        List<String> requiredIndexes = List.of(
                "idx_tenant_settings_lookup",
                "idx_branches_tenant",
                "idx_branch_settings_lookup",
                "idx_users_tenant_branch",
                "idx_users_email",
                "idx_categories_tenant",
                "idx_ingredients_tenant",
                "idx_dishes_tenant_cat",
                "idx_dishes_branch",
                "idx_recipes_dish",
                "idx_branch_dishes_lookup",
                "idx_stock_tenant_branch",
                "idx_purchases_tenant_branch",
                "idx_purchase_items_purchase",
                "idx_purchase_items_tenant_branch",
                "idx_kardex_lookup",
                "idx_orders_tenant_branch_status",
                "idx_orders_client_tx",
                "idx_order_items_order",
                "idx_order_items_tenant_branch",
                "idx_shifts_tenant_branch",
                "idx_orders_cash_shift",
                "idx_expenses_tenant_branch_date",
                "idx_expenses_shift_approval",
                "idx_audit_tenant_entity"
        );

        for (String index : requiredIndexes) {
            assertThat(sql).containsIgnoringCase(index);
        }

        // 3. Verify specific column requirements from TASK-002
        assertThat(sql).contains("timezone VARCHAR(50) NOT NULL DEFAULT 'America/La_Paz'");
        assertThat(sql).contains("uq_user_tenant_username UNIQUE (tenant_id, username)");
        assertThat(sql).contains("email VARCHAR(120)");
        assertThat(sql).contains("email_verified BOOLEAN NOT NULL DEFAULT FALSE");
        assertThat(sql).contains("cloned_from_id UUID REFERENCES dishes(id) ON DELETE SET NULL");
        assertThat(sql).contains("branch_id UUID REFERENCES branches(id) ON DELETE CASCADE");
        assertThat(sql).contains("client_transaction_id UUID NOT NULL UNIQUE");
        assertThat(sql).contains("cash_shift_id UUID");
        assertThat(sql).contains("paid_from_cash_drawer BOOLEAN NOT NULL DEFAULT FALSE");
        assertThat(sql).contains("approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'");
        assertThat(sql).contains("old_values JSONB");
        assertThat(sql).contains("new_values JSONB");

        // 4. Verify balanced parentheses
        long openParens = sql.chars().filter(ch -> ch == '(').count();
        long closeParens = sql.chars().filter(ch -> ch == ')').count();
        assertThat(openParens)
                .as("Parentheses must be balanced")
                .isEqualTo(closeParens);

        // 5. Verify domain check constraints
        assertThat(sql).contains("CHECK (value_type IN ('STRING', 'BOOLEAN', 'NUMBER', 'JSON'))");
        assertThat(sql).contains("CHECK (role IN ('SUPERADMIN', 'ADMIN_TENANT', 'BRANCH_MANAGER', 'CASHIER', 'KITCHEN'))");
        assertThat(sql).contains("CHECK (theme IN ('LIGHT', 'DARK'))");
        assertThat(sql).contains("CHECK (unit_of_measure IN ('KG', 'GRAM', 'LITER', 'ML', 'UNIT'))");
        assertThat(sql).contains("CHECK (status IN ('OPEN', 'CLOSED'))");
        assertThat(sql).contains("CHECK (expense_type IN ('DAILY_OPERATIONAL', 'MONTHLY_FIXED_PRORATED'))");

        // 6. Verify unique constraints
        assertThat(sql).contains("CONSTRAINT uq_tenant_setting_key UNIQUE (tenant_id, setting_key)");
        assertThat(sql).contains("CONSTRAINT uq_branch_setting_key UNIQUE (branch_id, setting_key)");
        assertThat(sql).contains("CONSTRAINT uq_dish_code_scope UNIQUE (tenant_id, branch_id, code)");
        assertThat(sql).contains("CONSTRAINT uq_dish_ingredient UNIQUE (dish_id, ingredient_id)");
        assertThat(sql).contains("CONSTRAINT uq_branch_dish UNIQUE (branch_id, dish_id)");
        assertThat(sql).contains("CONSTRAINT uq_branch_ingredient UNIQUE (branch_id, ingredient_id)");

        // 7. Verify foreign key rules
        assertThat(sql).contains("ALTER TABLE orders ADD CONSTRAINT fk_orders_cash_shift FOREIGN KEY (cash_shift_id) REFERENCES cash_shifts(id) ON DELETE SET NULL;");
        assertThat(sql).contains("ON DELETE RESTRICT");
        assertThat(sql).contains("ON DELETE SET NULL");
        assertThat(sql).contains("ON DELETE CASCADE");
    }
}
