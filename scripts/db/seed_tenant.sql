-- =============================================================================
-- SCRIPT DE SEMILLA BAJO DEMANDA PARA TENANCY NUEVO EN POSTGRESQL
-- Proyecto: Restaurant POS ERP
-- Uso: psql -U postgres -d restaurant_dev -f scripts/db/seed_tenant.sql
--      O ejecutar mediante: ./scripts/db/seed_tenant.sh restaurant_dev
-- =============================================================================

DO $$
DECLARE
    -- =========================================================================
    -- CONFIGURACIÓN DEL TENANCY (Modificar estos valores según la prueba)
    -- =========================================================================
    v_legal_name    TEXT := 'La Casona Gourmet S.R.L.';
    v_nit           TEXT := '55443322';
    v_slug          TEXT := 'la-casona';
    v_admin_user    TEXT := 'admin_casona';
    v_admin_email   TEXT := 'admin@lacasona.bo';
    v_cashier_user  TEXT := 'cajero_casona';
    v_currency      TEXT := 'BOB';
    v_timezone      TEXT := 'America/La_Paz';

    -- Hashes Argon2 pre-calculados (Spring Security Argon2PasswordEncoder):
    -- Contraseña 'admin123'
    v_hash_admin    TEXT := '$argon2id$v=19$m=16384,t=2,p=1$EfmkEqOKm/voCZ/IfxTwQw$Ik2yCsC3EmUrz0CDAc8jXeZxMHCrn21nDiTBh3B74DE';
    -- Contraseña 'cajero123'
    v_hash_cajero   TEXT := '$argon2id$v=19$m=16384,t=2,p=1$u6rwybZT9hjcq43nZspsjg$p7Nl3jmqHDiaOtptJocEpJxFC1WtfmmEoCBACiUuWa4';

    -- Identificadores generados
    v_tenant_id     UUID := gen_random_uuid();
    v_branch1_id    UUID := gen_random_uuid();
    v_branch2_id    UUID := gen_random_uuid();
    v_admin_id      UUID := gen_random_uuid();
    v_cashier_id    UUID := gen_random_uuid();
    v_role_admin_id UUID := gen_random_uuid();
    v_role_cash_id  UUID := gen_random_uuid();

    -- Categorías
    v_cat_burgers   UUID := gen_random_uuid();
    v_cat_sides     UUID := gen_random_uuid();
    v_cat_drinks    UUID := gen_random_uuid();

    -- Insumos
    v_ing_carne     UUID := gen_random_uuid();
    v_ing_pan       UUID := gen_random_uuid();
    v_ing_queso     UUID := gen_random_uuid();
    v_ing_papas     UUID := gen_random_uuid();
    v_ing_gaseosa   UUID := gen_random_uuid();

    -- Platos
    v_dish_burger   UUID := gen_random_uuid();
    v_dish_papas    UUID := gen_random_uuid();
    v_dish_gaseosa  UUID := gen_random_uuid();

    -- Modificadores
    v_mod_group_coc UUID := gen_random_uuid();
    v_mod_coc_medio UUID := gen_random_uuid();
    v_mod_coc_bien  UUID := gen_random_uuid();

    -- Menú del Día
    v_menu_id       UUID := gen_random_uuid();

    -- Turno de Caja
    v_shift1_id     UUID := gen_random_uuid();
    v_shift2_id     UUID := gen_random_uuid();

BEGIN
    -- Evitar duplicados si el NIT ya existe
    IF EXISTS (SELECT 1 FROM tenants WHERE nit_or_tax_id = v_nit) THEN
        RAISE NOTICE 'El tenant con NIT % ya existe. Abortando seed para evitar duplicación.', v_nit;
        RETURN;
    END IF;

    RAISE NOTICE '==> Creando Tenancy: % (NIT: %, Slug: %)...', v_legal_name, v_nit, v_slug;

    -- 1. Tenant
    INSERT INTO tenants (id, legal_name, nit_or_tax_id, slug, is_active, created_at, updated_at, version)
    VALUES (v_tenant_id, v_legal_name, v_nit, v_slug, TRUE, NOW(), NOW(), 0);

    -- 2. Sucursales (Central y Norte)
    INSERT INTO branches (id, tenant_id, name, address, phone, timezone, currency, is_active, created_at, version)
    VALUES
        (v_branch1_id, v_tenant_id, 'Sucursal Central', 'Av. Principal 123', '70012345', v_timezone, v_currency, TRUE, NOW(), 0),
        (v_branch2_id, v_tenant_id, 'Sucursal Norte', 'Calle 5 Zona Norte', '70098765', v_timezone, v_currency, TRUE, NOW(), 0);

    -- 3. Parámetros de Configuración Dinámica
    INSERT INTO tenant_settings (id, tenant_id, setting_key, setting_value, value_type, description, updated_at)
    VALUES
        (gen_random_uuid(), v_tenant_id, 'INVENTORY_DEDUCTION_MODE', 'AUTOMATIC_PERMISSIVE', 'STRING', 'Deducción de insumos en Kardex', NOW()),
        (gen_random_uuid(), v_tenant_id, 'REQUIRE_USER_EMAIL', 'false', 'BOOLEAN', 'Permite crear cajeros sin email', NOW()),
        (gen_random_uuid(), v_tenant_id, 'DEFAULT_DISH_INVENTORY_STRATEGY', 'RECIPE_BOM_STRICT', 'STRING', 'Estrategia de inventario por defecto', NOW());

    INSERT INTO branch_settings (id, tenant_id, branch_id, setting_key, setting_value, value_type, description, updated_at)
    VALUES
        (gen_random_uuid(), v_tenant_id, v_branch1_id, 'AUTO_PRINT_TICKETS', 'true', 'BOOLEAN', 'Impresión automática', NOW()),
        (gen_random_uuid(), v_tenant_id, v_branch1_id, 'MAX_CASH_EXPENSE_LIMIT', '300.00', 'NUMBER', 'Tope gasto caja', NOW()),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, 'AUTO_PRINT_TICKETS', 'true', 'BOOLEAN', 'Impresión automática', NOW()),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, 'MAX_CASH_EXPENSE_LIMIT', '300.00', 'NUMBER', 'Tope gasto caja', NOW());

    -- 4. Roles RBAC y Permisos
    INSERT INTO roles (id, tenant_id, code, name, description, is_system, created_at)
    VALUES
        (v_role_admin_id, v_tenant_id, 'ADMIN_TENANT', 'Administrador de Empresa', 'Acceso total gerencial', TRUE, NOW()),
        (v_role_cash_id, v_tenant_id, 'CASHIER', 'Cajero de Salón', 'Operación de turnos y punto de venta', TRUE, NOW());

    -- 5. Usuarios
    INSERT INTO app_users (id, tenant_id, branch_id, role_id, full_name, username, email, password_hash, role, email_verified, is_active, created_at, version)
    VALUES
        (v_admin_id, v_tenant_id, v_branch1_id, v_role_admin_id, 'Administrador Casona', v_admin_user, v_admin_email, v_hash_admin, 'ADMIN_TENANT', TRUE, TRUE, NOW(), 0),
        (v_cashier_id, v_tenant_id, v_branch1_id, v_role_cash_id, 'Cajero Turno Casona', v_cashier_user, NULL, v_hash_cajero, 'CASHIER', FALSE, TRUE, NOW(), 0);

    -- 5.1 Asignación multi-sucursal en user_branches
    INSERT INTO user_branches (id, tenant_id, user_id, branch_id, created_at)
    VALUES
        (gen_random_uuid(), v_tenant_id, v_admin_id, v_branch1_id, NOW()),
        (gen_random_uuid(), v_tenant_id, v_admin_id, v_branch2_id, NOW()),
        (gen_random_uuid(), v_tenant_id, v_cashier_id, v_branch1_id, NOW());

    -- 6. Categorías de Catálogo
    INSERT INTO categories (id, tenant_id, name, icon, sort_order, code_prefix, created_at)
    VALUES
        (v_cat_burgers, v_tenant_id, 'Hamburguesas y Platos', 'burger', 1, 'HAM', NOW()),
        (v_cat_sides, v_tenant_id, 'Guarniciones & Entradas', 'fries', 2, 'GUA', NOW()),
        (v_cat_drinks, v_tenant_id, 'Bebidas y Refrescos', 'cup', 3, 'BEB', NOW());

    -- 7. Insumos Base
    INSERT INTO ingredients (id, tenant_id, name, unit_of_measure, min_stock_alert, created_at)
    VALUES
        (v_ing_carne, v_tenant_id, 'Carne de Res Molida Especial', 'KG', 10.00, NOW()),
        (v_ing_pan, v_tenant_id, 'Pan Brioche Artesanal', 'UNIT', 20.00, NOW()),
        (v_ing_queso, v_tenant_id, 'Queso Cheddar Madurado', 'KG', 5.00, NOW()),
        (v_ing_papas, v_tenant_id, 'Papas Rústicas', 'KG', 15.00, NOW()),
        (v_ing_gaseosa, v_tenant_id, 'Gaseosa Personal 500ml', 'UNIT', 20.00, NOW());

    -- 8. Modificadores
    INSERT INTO modifier_groups (id, tenant_id, name, min_selectable, max_selectable, is_required, created_at)
    VALUES (v_mod_group_coc, v_tenant_id, 'Cocción de la Carne', 1, 1, TRUE, NOW());

    INSERT INTO modifiers (id, tenant_id, group_id, name, additional_price, ingredient_id, quantity_consumed, is_active, sort_order)
    VALUES
        (v_mod_coc_medio, v_tenant_id, v_mod_group_coc, 'Término Medio', 0.00, NULL, 0, TRUE, 1),
        (v_mod_coc_bien, v_tenant_id, v_mod_group_coc, 'Bien Cocido', 0.00, NULL, 0, TRUE, 2);

    -- 9. Platos y Recetas (BOM)
    INSERT INTO dishes (id, tenant_id, category_id, code, name, description, sale_price, is_active, inventory_strategy, created_at, version)
    VALUES
        (v_dish_burger, v_tenant_id, v_cat_burgers, 'CAS-BURG-01', 'Hamburguesa de la Casa', 'Carne 180g, pan brioche y queso cheddar', 40.00, TRUE, 'RECIPE_BOM_STRICT', NOW(), 0),
        (v_dish_papas, v_tenant_id, v_cat_sides, 'CAS-FRIES-01', 'Papas Fritas Rústicas', 'Porción generosa de papas con sal marina', 18.00, TRUE, 'RECIPE_BOM_STRICT', NOW(), 0),
        (v_dish_gaseosa, v_tenant_id, v_cat_drinks, 'CAS-BEV-01', 'Gaseosa 500ml', 'Refresco personal frío', 12.00, TRUE, 'COMMERCIAL_NO_INVENTORY', NOW(), 0);

    INSERT INTO dish_modifier_groups (id, dish_id, group_id, sort_order)
    VALUES (gen_random_uuid(), v_dish_burger, v_mod_group_coc, 1);

    INSERT INTO dish_recipes (id, dish_id, ingredient_id, quantity)
    VALUES
        (gen_random_uuid(), v_dish_burger, v_ing_carne, 0.1800),
        (gen_random_uuid(), v_dish_burger, v_ing_pan, 1.0000),
        (gen_random_uuid(), v_dish_burger, v_ing_queso, 0.0400),
        (gen_random_uuid(), v_dish_papas, v_ing_papas, 0.3000);

    -- 10. Plantilla de Menú del Día (Package)
    INSERT INTO daily_menus (id, tenant_id, branch_id, name, description, day_of_week, price_mode, package_price, is_template, is_active, created_at, updated_at, version)
    VALUES (v_menu_id, v_tenant_id, NULL, 'Almuerzo Ejecutivo Casona', 'Entrada + Plato Fuerte + Bebida', 'Lunes a Viernes', 'FIXED_PACKAGE', 45.00, TRUE, TRUE, NOW(), NOW(), 0);

    INSERT INTO daily_menu_items (id, menu_id, dish_id, course_type, suggested_quota, override_price, is_mandatory_in_package, sort_order)
    VALUES
        (gen_random_uuid(), v_menu_id, v_dish_papas, 'STARTER', 25, NULL, FALSE, 1),
        (gen_random_uuid(), v_menu_id, v_dish_burger, 'MAIN', 25, NULL, TRUE, 2),
        (gen_random_uuid(), v_menu_id, v_dish_gaseosa, 'BEVERAGE', 25, NULL, FALSE, 3);

    -- 11. Stock Inicial de Insumos (Ambas sucursales)
    INSERT INTO branch_stock (id, tenant_id, branch_id, ingredient_id, current_quantity, average_unit_cost, last_updated, version)
    VALUES
        (gen_random_uuid(), v_tenant_id, v_branch1_id, v_ing_carne, 80.0000, 30.00, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch1_id, v_ing_pan, 120.0000, 2.50, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch1_id, v_ing_queso, 40.0000, 45.00, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch1_id, v_ing_papas, 100.0000, 8.00, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, v_ing_carne, 80.0000, 30.00, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, v_ing_pan, 120.0000, 2.50, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, v_ing_queso, 40.0000, 45.00, NOW(), 0),
        (gen_random_uuid(), v_tenant_id, v_branch2_id, v_ing_papas, 100.0000, 8.00, NOW(), 0);

    -- 12. Apertura Inmediata de Turnos de Caja (OPEN) con Fondo Inicial ($Bs. 200.00)
    INSERT INTO cash_shifts (id, tenant_id, branch_id, opened_by, opened_at, initial_cash, status)
    VALUES
        (v_shift1_id, v_tenant_id, v_branch1_id, v_admin_id, NOW(), 200.00, 'OPEN'),
        (v_shift2_id, v_tenant_id, v_branch2_id, v_admin_id, NOW(), 200.00, 'OPEN');

    RAISE NOTICE '==> Tenancy creado exitosamente!';
    RAISE NOTICE '    Tenant ID: %', v_tenant_id;
    RAISE NOTICE '    Admin:   % / admin123', v_admin_user;
    RAISE NOTICE '    Cajero:  % / cajero123', v_cashier_user;
    RAISE NOTICE '    Sucursal Central ID: % (Turno OPEN)', v_branch1_id;
    RAISE NOTICE '    Sucursal Norte ID:   % (Turno OPEN)', v_branch2_id;

END $$;
