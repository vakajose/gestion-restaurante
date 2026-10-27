package com.restaurant.app.core.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Catálogo maestro de permisos granulares por recurso del sistema (recurso:acción).
 * Permite diseñar roles personalizados a nivel de empresa con asignación precisa de accesos.
 */
public final class SystemPermissions {

    private SystemPermissions() {}

    // 1. Empresa y Sucursales
    public static final String COMPANY_READ = "company:read";
    public static final String COMPANY_WRITE = "company:write";
    public static final String BRANCHES_READ = "branches:read";
    public static final String BRANCHES_WRITE = "branches:write";

    // 2. Usuarios y Roles
    public static final String USERS_READ = "users:read";
    public static final String USERS_WRITE = "users:write";
    public static final String ROLES_MANAGE = "roles:manage";

    // 3. Catálogo y Recetas
    public static final String CATALOG_READ = "catalog:read";
    public static final String CATALOG_WRITE = "catalog:write";

    // 4. Punto de Venta (POS)
    public static final String POS_ACCESS = "pos:access";
    public static final String POS_ORDER_CREATE = "pos:order_create";
    public static final String POS_DISCOUNT_APPLY = "pos:discount_apply";

    // 5. Inventario y Kardex
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_STOCK_INITIAL = "inventory:stock_initial";
    public static final String INVENTORY_PURCHASE = "inventory:purchase";
    public static final String INVENTORY_ADJUST = "inventory:adjust";

    // 6. Finanzas y Turnos
    public static final String FINANCE_SHIFTS = "finance:shifts";
    public static final String FINANCE_EXPENSES_CREATE = "finance:expenses_create";
    public static final String FINANCE_EXPENSES_APPROVE = "finance:expenses_approve";
    public static final String FINANCE_REPORTS = "finance:reports";

    public static final Set<String> ALL_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
        COMPANY_READ, COMPANY_WRITE,
        BRANCHES_READ, BRANCHES_WRITE,
        USERS_READ, USERS_WRITE, ROLES_MANAGE,
        CATALOG_READ, CATALOG_WRITE,
        POS_ACCESS, POS_ORDER_CREATE, POS_DISCOUNT_APPLY,
        INVENTORY_READ, INVENTORY_STOCK_INITIAL, INVENTORY_PURCHASE, INVENTORY_ADJUST,
        FINANCE_SHIFTS, FINANCE_EXPENSES_CREATE, FINANCE_EXPENSES_APPROVE, FINANCE_REPORTS
    )));

    public static Set<String> getDefaultPermissionsForRole(String roleCode) {
        if (roleCode == null) {
            return Collections.emptySet();
        }

        return switch (roleCode.toUpperCase()) {
            case "SUPERADMIN", "ADMIN_TENANT" -> ALL_PERMISSIONS;
            case "BRANCH_MANAGER" -> Set.of(
                BRANCHES_READ, USERS_READ,
                CATALOG_READ,
                POS_ACCESS, POS_ORDER_CREATE, POS_DISCOUNT_APPLY,
                INVENTORY_READ, INVENTORY_STOCK_INITIAL, INVENTORY_PURCHASE, INVENTORY_ADJUST,
                FINANCE_SHIFTS, FINANCE_EXPENSES_CREATE, FINANCE_EXPENSES_APPROVE
            );
            case "CASHIER" -> Set.of(
                CATALOG_READ,
                POS_ACCESS, POS_ORDER_CREATE,
                FINANCE_SHIFTS, FINANCE_EXPENSES_CREATE
            );
            case "KITCHEN" -> Set.of(
                CATALOG_READ,
                POS_ACCESS
            );
            default -> Collections.emptySet();
        };
    }
}
