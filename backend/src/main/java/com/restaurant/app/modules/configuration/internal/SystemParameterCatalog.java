package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterCriticality;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.ParameterType;
import com.restaurant.app.modules.configuration.api.SystemParameter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SystemParameterCatalog {

    record ParameterDefinition(
        String key,
        String name,
        String description,
        ParameterCategory category,
        ParameterType type,
        String defaultValue,
        ParameterScope allowedScope,
        ParameterCriticality criticality,
        boolean requiresRestart,
        List<String> options
    ) {}

    private static final Map<String, ParameterDefinition> CATALOG = new LinkedHashMap<>();

    static {
        register(new ParameterDefinition(
            SystemParameter.INVENTORY_DEDUCTION_MODE.getKey(),
            "Modo de Deducción de Inventario",
            "Define la política de descuento de existencias en Kardex al procesar órdenes (MANUAL_ISOLATED, AUTOMATIC_PERMISSIVE, AUTOMATIC_STRICT)",
            ParameterCategory.INVENTORY,
            ParameterType.ENUM,
            "AUTOMATIC_PERMISSIVE",
            ParameterScope.TENANT,
            ParameterCriticality.HIGH,
            false,
            List.of("MANUAL_ISOLATED", "AUTOMATIC_PERMISSIVE", "AUTOMATIC_STRICT")
        ));

        register(new ParameterDefinition(
            SystemParameter.INVENTORY_ALLOW_NEGATIVE_STOCK.getKey(),
            "Permitir Stock Negativo en Inventario",
            "Permite ventas aun sin existencias suficientes en almacén generando advertencias de déficit (ADR-0015)",
            ParameterCategory.INVENTORY,
            ParameterType.BOOLEAN,
            "false",
            ParameterScope.TENANT,
            ParameterCriticality.HIGH,
            false,
            null
        ));

        register(new ParameterDefinition(
            SystemParameter.POS_DISPLAY_DISH_IMAGES.getKey(),
            "Mostrar Imágenes en Pantalla POS",
            "Alterna entre botonera visual con imágenes o cuadrícula compacta de alta velocidad para mostrador",
            ParameterCategory.POS,
            ParameterType.BOOLEAN,
            "true",
            ParameterScope.BRANCH,
            ParameterCriticality.LOW,
            false,
            List.of("true", "false")
        ));

        register(new ParameterDefinition(
            SystemParameter.POS_DEFAULT_ORDER_MODE.getKey(),
            "Modalidad de Comanda por Defecto",
            "Modalidad inicial de servicio para nuevas comandas en el terminal de venta (DINE_IN, TAKEAWAY, TAKEOUT, DELIVERY)",
            ParameterCategory.POS,
            ParameterType.ENUM,
            "DINE_IN",
            ParameterScope.BRANCH,
            ParameterCriticality.LOW,
            false,
            List.of("DINE_IN", "TAKEAWAY", "TAKEOUT", "DELIVERY")
        ));

        register(new ParameterDefinition(
            SystemParameter.POS_ALLOW_SCHEDULED_PICKUP.getKey(),
            "Permitir Pedidos con Horario Pactado",
            "Habilita la programación de pedidos para retiro anticipado en mostrador o entrega futura",
            ParameterCategory.POS,
            ParameterType.BOOLEAN,
            "false",
            ParameterScope.BRANCH,
            ParameterCriticality.LOW,
            false,
            List.of("true", "false")
        ));

        register(new ParameterDefinition(
            SystemParameter.FINANCE_MAX_PETTY_CASH_EXPENSE.getKey(),
            "Límite Máximo de Gasto Menor sin Autorización",
            "Monto máximo en moneda local que el cajero puede registrar como egreso de caja menor sin PIN de supervisor",
            ParameterCategory.FINANCE,
            ParameterType.DOUBLE,
            "100.00",
            ParameterScope.BRANCH,
            ParameterCriticality.MEDIUM,
            false,
            List.of()
        ));

        register(new ParameterDefinition(
            SystemParameter.FINANCE_REQUIRE_SUPERVISOR_PIN_CANCEL.getKey(),
            "Exigir PIN de Supervisor para Anulación de Comanda",
            "Obliga a que un encargado ingrese su PIN para autorizar la anulación de tickets o platos ya comandados",
            ParameterCategory.FINANCE,
            ParameterType.BOOLEAN,
            "true",
            ParameterScope.TENANT,
            ParameterCriticality.HIGH,
            false,
            List.of("true", "false")
        ));

        register(new ParameterDefinition(
            SystemParameter.FINANCE_SHIFT_OPENING_POLICY.getKey(),
            "Política de Apertura de Turno de Caja",
            "Determina quién puede abrir la caja de la sucursal (ALL_AUTHORIZED o SUPERVISOR_ONLY)",
            ParameterCategory.FINANCE,
            ParameterType.ENUM,
            "ALL_AUTHORIZED",
            ParameterScope.BRANCH,
            ParameterCriticality.MEDIUM,
            false,
            List.of("ALL_AUTHORIZED", "SUPERVISOR_ONLY")
        ));

        register(new ParameterDefinition(
            SystemParameter.FINANCE_EXPENSE_ALLOW_SELF_APPROVAL.getKey(),
            "Permitir Autoaprobación de Gastos Menores",
            "Si es falso, un usuario con permiso de aprobación que registre un gasto propio requerirá control dual (PENDING_APPROVAL)",
            ParameterCategory.FINANCE,
            ParameterType.BOOLEAN,
            "false",
            ParameterScope.BRANCH,
            ParameterCriticality.MEDIUM,
            false,
            List.of("true", "false")
        ));

        register(new ParameterDefinition(
            SystemParameter.SECURITY_REQUIRE_USER_EMAIL.getKey(),
            "Exigir Correo Electrónico para Colaboradores",
            "Determina si el correo electrónico es obligatorio para operadores operativos (cajeros, cocineros)",
            ParameterCategory.SECURITY,
            ParameterType.BOOLEAN,
            "true",
            ParameterScope.TENANT,
            ParameterCriticality.HIGH,
            false,
            List.of("true", "false")
        ));

        register(new ParameterDefinition(
            SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES.getKey(),
            "Tiempo de Inactividad de Sesión (Minutos)",
            "Minutos continuos sin actividad antes de cerrar la sesión activa y solicitar reautenticación",
            ParameterCategory.SECURITY,
            ParameterType.INTEGER,
            "15",
            ParameterScope.TENANT,
            ParameterCriticality.MEDIUM,
            false,
            List.of()
        ));

        register(new ParameterDefinition(
            SystemParameter.COMPANY_CURRENCY.getKey(),
            "Moneda de Operación",
            "Código ISO de la moneda principal para fijación de precios y arqueo de caja (ej. BOB, USD)",
            ParameterCategory.GENERAL,
            ParameterType.STRING,
            "BOB",
            ParameterScope.BRANCH,
            ParameterCriticality.MEDIUM,
            false,
            List.of()
        ));

        register(new ParameterDefinition(
            SystemParameter.COMPANY_TIMEZONE.getKey(),
            "Huso Horario de Sucursal",
            "Identificador IANA de zona horaria para cortes de turno y agrupación de ventas diarias",
            ParameterCategory.GENERAL,
            ParameterType.STRING,
            "America/La_Paz",
            ParameterScope.BRANCH,
            ParameterCriticality.MEDIUM,
            false,
            List.of()
        ));
    }

    private static void register(ParameterDefinition definition) {
        CATALOG.put(definition.key().toLowerCase(), definition);
    }

    public static Optional<ParameterDefinition> getDefinition(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATALOG.get(key.trim().toLowerCase()));
    }

    public static List<ParameterDefinition> getAllDefinitions() {
        return List.copyOf(CATALOG.values());
    }

    public static List<ParameterDefinition> getDefinitionsByCategory(ParameterCategory category) {
        if (category == null) {
            return getAllDefinitions();
        }
        return CATALOG.values().stream()
            .filter(def -> def.category() == category)
            .toList();
    }

    public static void validate(ParameterDefinition def, ParameterScope targetScope, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El valor del parámetro no puede estar vacío");
        }

        // Validación de jerarquía de ámbito
        if (targetScope == ParameterScope.BRANCH && def.allowedScope() == ParameterScope.TENANT) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' no permite sobreescrituras a nivel de sucursal (exclusivo de empresa)");
        }
        if (targetScope == ParameterScope.BRANCH && def.allowedScope() == ParameterScope.GLOBAL) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' solo puede configurarse a nivel global");
        }
        if (targetScope == ParameterScope.TENANT && def.allowedScope() == ParameterScope.GLOBAL) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' solo puede configurarse a nivel global");
        }

        // Validación de tipo de dato
        String trimmed = value.trim();
        switch (def.type()) {
            case BOOLEAN -> {
                if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser booleano ('true' o 'false')");
                }
            }
            case INTEGER -> {
                try {
                    Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser un número entero válido");
                }
            }
            case DOUBLE -> {
                try {
                    Double.parseDouble(trimmed);
                } catch (NumberFormatException e) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser un número decimal válido");
                }
            }
            case ENUM -> {
                if (def.options() != null && !def.options().isEmpty()) {
                    boolean matches = def.options().stream().anyMatch(opt -> opt.equalsIgnoreCase(trimmed));
                    if (!matches) {
                        throw new ValidationException("El valor '" + trimmed + "' no es válido para '" + def.key() +
                            "'. Opciones permitidas: " + def.options());
                    }
                }
            }
            case STRING, JSON -> {
                // Validación básica de no vacío
            }
        }
    }
}
