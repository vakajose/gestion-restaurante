package com.restaurant.app.core.security;

import com.restaurant.app.core.security.api.RolePublicApi.PermissionCatalogItemDto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
    public static final String SETTINGS_READ = "settings:read";
    public static final String SETTINGS_WRITE = "settings:write";

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
    public static final String POS_VOID_ITEM = "pos:void_item";
    public static final String POS_KDS = "pos:kds";

    // 5. Inventario y Kardex
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_STOCK_INITIAL = "inventory:stock_initial";
    public static final String INVENTORY_PURCHASE = "inventory:purchase";
    public static final String INVENTORY_ADJUST = "inventory:adjust";

    // 6. Finanzas y Turnos
    public static final String FINANCE_SHIFTS = "finance:shifts";
    public static final String FINANCE_SHIFTS_SUPERVISOR = "finance:shifts_supervisor";
    public static final String FINANCE_EXPENSES_CREATE = "finance:expenses_create";
    public static final String FINANCE_EXPENSES_APPROVE = "finance:expenses_approve";
    public static final String FINANCE_REPORTS = "finance:reports";
    public static final String REPORTS_VIEW = "reports:view";

    public static final Set<String> ALL_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Set.of(
        COMPANY_READ, COMPANY_WRITE,
        BRANCHES_READ, BRANCHES_WRITE,
        SETTINGS_READ, SETTINGS_WRITE,
        USERS_READ, USERS_WRITE, ROLES_MANAGE,
        CATALOG_READ, CATALOG_WRITE,
        POS_ACCESS, POS_ORDER_CREATE, POS_DISCOUNT_APPLY, POS_VOID_ITEM, POS_KDS,
        INVENTORY_READ, INVENTORY_STOCK_INITIAL, INVENTORY_PURCHASE, INVENTORY_ADJUST,
        FINANCE_SHIFTS, FINANCE_SHIFTS_SUPERVISOR, FINANCE_EXPENSES_CREATE, FINANCE_EXPENSES_APPROVE, FINANCE_REPORTS, REPORTS_VIEW
    )));

    private static final List<PermissionCatalogItemDto> CATALOG_ITEMS = List.of(
        // POS
        new PermissionCatalogItemDto(POS_ACCESS, "Acceso a Terminal POS", "Permite ingresar a la pantalla de Punto de Venta", "POS"),
        new PermissionCatalogItemDto(POS_ORDER_CREATE, "Crear y Cobrar Comandas", "Permite registrar nuevos pedidos y procesar cobros", "POS"),
        new PermissionCatalogItemDto(POS_DISCOUNT_APPLY, "Aplicar Descuentos y Cortesías", "Autoriza aplicar descuentos manuales o promociones sobre comandas", "POS"),
        new PermissionCatalogItemDto(POS_VOID_ITEM, "Anular Ítems o Tickets", "Permite anular líneas de comanda ya enviadas a cocina", "POS"),
        new PermissionCatalogItemDto(POS_KDS, "Monitor de Cocina (KDS)", "Permite ver y despachar comandas en el monitor de cocina/barra", "POS"),

        // INVENTORY
        new PermissionCatalogItemDto(INVENTORY_READ, "Consultar Existencias y Kardex", "Permite visualizar el inventario actual y el historial del Kardex", "INVENTORY"),
        new PermissionCatalogItemDto(INVENTORY_STOCK_INITIAL, "Cargar Inventario Inicial", "Permite registrar saldos iniciales de insumos en almacén", "INVENTORY"),
        new PermissionCatalogItemDto(INVENTORY_PURCHASE, "Registrar Compras a Proveedores", "Permite ingresar facturas de compra de insumos", "INVENTORY"),
        new PermissionCatalogItemDto(INVENTORY_ADJUST, "Ajustes y Mermas de Almacén", "Permite registrar bajas por merma o conteos físicos", "INVENTORY"),

        // FINANCE
        new PermissionCatalogItemDto(FINANCE_SHIFTS, "Apertura y Cierre de Caja", "Permite abrir turno, realizar arqueo y cerrar caja chica", "FINANCE"),
        new PermissionCatalogItemDto(FINANCE_SHIFTS_SUPERVISOR, "Supervisión y Apertura de Turnos", "Permite aperturar turnos bajo política estricta de supervisión", "FINANCE"),
        new PermissionCatalogItemDto(FINANCE_EXPENSES_CREATE, "Registrar Gastos Menores", "Permite registrar salidas de efectivo de gaveta o gastos operativos", "FINANCE"),
        new PermissionCatalogItemDto(FINANCE_EXPENSES_APPROVE, "Aprobar Gastos de Caja Chica", "Permite autorizar o rechazar gastos registrados por cajeros", "FINANCE"),
        new PermissionCatalogItemDto(FINANCE_REPORTS, "Reportes Financieros y Márgenes", "Permite consultar estados de resultados y rentabilidad", "FINANCE"),
        new PermissionCatalogItemDto(REPORTS_VIEW, "Analítica Operativa", "Permite visualizar tableros e indicadores de ventas", "REPORTS"),

        // CATALOG
        new PermissionCatalogItemDto(CATALOG_READ, "Ver Catálogo y Recetas", "Permite consultar categorías, platos, combos e insumos", "CATALOG"),
        new PermissionCatalogItemDto(CATALOG_WRITE, "Administrar Catálogo y Precios", "Permite crear o editar platos, recetas (BOM), modificadores y precios", "CATALOG"),

        // ADMIN / USERS / COMPANY / CONFIG
        new PermissionCatalogItemDto(COMPANY_READ, "Ver Datos de Empresa", "Permite visualizar información fiscal y corporativa", "ADMIN"),
        new PermissionCatalogItemDto(COMPANY_WRITE, "Editar Datos de Empresa", "Permite modificar razón social, NIT, slug y logotipo", "ADMIN"),
        new PermissionCatalogItemDto(BRANCHES_READ, "Ver Sucursales", "Permite listar sucursales de la empresa", "ADMIN"),
        new PermissionCatalogItemDto(BRANCHES_WRITE, "Administrar Sucursales", "Permite crear, editar o activar/desactivar sucursales", "ADMIN"),
        new PermissionCatalogItemDto(USERS_READ, "Ver Personal y Colaboradores", "Permite consultar el listado de usuarios de la empresa", "ADMIN"),
        new PermissionCatalogItemDto(USERS_WRITE, "Administrar Personal y Contraseñas", "Permite crear usuarios, asignar sucursales y resetear claves", "ADMIN"),
        new PermissionCatalogItemDto(ROLES_MANAGE, "Administrar Roles y Permisos", "Permite crear y configurar roles personalizados RBAC", "ADMIN"),
        new PermissionCatalogItemDto(SETTINGS_READ, "Consultar Parámetros Operativos", "Permite ver la configuración de empresa y sucursal", "CONFIG"),
        new PermissionCatalogItemDto(SETTINGS_WRITE, "Modificar Parámetros Operativos", "Permite cambiar reglas operativas de POS, inventario y seguridad", "CONFIG")
    );

    public static List<PermissionCatalogItemDto> getPermissionsCatalog() {
        return CATALOG_ITEMS;
    }

    public static Set<String> getDefaultPermissionsForRole(String roleCode) {
        if (roleCode == null) {
            return Collections.emptySet();
        }

        return switch (roleCode.toUpperCase()) {
            case "SUPERADMIN", "ADMIN_TENANT" -> ALL_PERMISSIONS;
            case "BRANCH_MANAGER" -> Set.of(
                BRANCHES_READ, USERS_READ, USERS_WRITE,
                SETTINGS_READ,
                CATALOG_READ,
                POS_ACCESS, POS_ORDER_CREATE, POS_DISCOUNT_APPLY, POS_VOID_ITEM, POS_KDS,
                INVENTORY_READ, INVENTORY_STOCK_INITIAL, INVENTORY_PURCHASE, INVENTORY_ADJUST,
                FINANCE_SHIFTS, FINANCE_SHIFTS_SUPERVISOR, FINANCE_EXPENSES_CREATE, FINANCE_EXPENSES_APPROVE, REPORTS_VIEW
            );
            case "CASHIER" -> Set.of(
                BRANCHES_READ,
                CATALOG_READ,
                POS_ACCESS, POS_ORDER_CREATE,
                FINANCE_SHIFTS, FINANCE_EXPENSES_CREATE
            );
            case "KITCHEN" -> Set.of(
                BRANCHES_READ,
                CATALOG_READ,
                POS_ACCESS, POS_KDS
            );
            default -> Collections.emptySet();
        };
    }
}
