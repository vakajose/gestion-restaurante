---
title: "ADR 0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica"
category: "adr"
tags:
  - adr
  - architecture
  - multi-tenant
  - configuration
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica


## Contexto
El sistema atiende tanto a restaurantes individuales (una sola sucursal) como a cadenas gastronómicas con múltiples sucursales. Además, el mercado objetivo abarca desde micro/pequeñas empresas que operan de manera informal (usuarios sin correo electrónico corporativo, inventario manual no automatizado por comanda) hasta medianas empresas con controles estrictos. 

Si el esquema de base de datos impone columnas rígidas para cada opción de configuración o si fuerza correos electrónicos obligatorios y reglas inflexibles de inventario, el producto se vuelve incompatible con la realidad operativa de sus usuarios o exigirá migraciones constantes de base de datos por cada nuevo parámetro.

## Decisión
1. **Jerarquía Dual de Entidades:**
   - `BaseTenantEntity` (`tenantId`, `createdAt`, `updatedAt`, `version`) para catálogo maestro y entidades globales.
   - `BaseBranchEntity extends BaseTenantEntity` (`branchId`) para transacciones y operaciones de sucursal.
2. **Denormalización de Índices de Alto Rendimiento:**
   - Se incluyen `tenant_id` y `branch_id` en tablas transaccionales hijas (`order_items`, `purchase_items`) para optimizar índices compuestos e independizar consultas sin joins innecesarios.
3. **Identidad Adaptativa:**
   - `username` es la credencial obligatoria y única por tenant (`uq_user_tenant_username`).
   - `email` es opcional (nullable), permitiendo dar de alta cajeros u operadores sin correo.
4. **Configuración Dinámica Clave-Valor:**
   - Se crean `tenant_settings` y `branch_settings` con pares `(setting_key, setting_value, value_type)`.
   - Se abstrae el acceso mediante `TenantConfigService` con valores por defecto en código.

## Consecuencias
- **Positivas:** Máxima flexibilidad para incorporar nuevos parámetros sin alterar el esquema relacional; adaptación total al mercado PyME; alto rendimiento en consultas multi-sucursal.
- **Negativas:** La configuración clave-valor requiere tipado y validación rigurosa en la capa de servicio backend.

## Enmienda y Estado Actual (Parcialmente Superado por ADR-0008 y ADR-0027)
> [!NOTE]
> La sección de configuración dinámica (`tenant_settings`, `branch_settings` y `TenantConfigService`) fue **parcialmente superada por [ADR-0008](./0008_unified_multilevel_parameter_system.md)** (tabla unificada `system_parameter_values`, catálogo `parameters-catalog.json` vía [ADR-0025](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md) y `ConfigurationPublicApi`) y por [ADR-0027](./0027_platform_identity_and_clean_golive_bootstrap.md) / [ADR-0028](./0028_agnostic_media_storage_and_platform_settings.md) (`platform_settings` para configuraciones globales SaaS).
>
> **Deuda técnica conocida vigente:** En la implementación actual de backend, `KardexService` todavía resuelve `INVENTORY_DEDUCTION_MODE` leyendo mediante `TenantConfigService`, mientras que `inventory.allow_negative_stock` ya consume `ConfigurationPublicApi` (unificación pendiente bajo `TASK-640`).

## Documentos Relacionados
- [ADR-0001: Monolito Modular con Spring Modulith](./0001_spring_modulith_monolith.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0009: Onboarding Guiado de Empresa y Presets Operativos](./0009_tenant_onboarding_and_operational_presets.md)
- [ADR-0015: Menús del Día y Estrategias Flexibles de Inventario](./0015_daily_menus_and_flexible_dish_inventory.md)
- [ADR-0025: Humanización de Parámetros y Catálogo JSON](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md)
- [ADR-0027: Identidad de Plataforma y Bootstrap Limpio](./0027_platform_identity_and_clean_golive_bootstrap.md)
- [ADR-0028: Almacenamiento Agnóstico de Media y `platform_settings`](./0028_agnostic_media_storage_and_platform_settings.md)
- [ADR-0030: Aislamiento del Catálogo por Sucursal por Defecto](./0030_catalog_branch_isolation_by_default.md)
- [Casos de Uso de Empresa y Sucursales](../project/architecture/03-use-cases/use_cases_company.md)
- [Casos de Uso de Configuración](../project/architecture/03-use-cases/use_cases_configuration.md)
- [Casos de Uso de Inventario](../project/architecture/03-use-cases/use_cases_inventory.md)
- [Modelo de Entidad-Relación](../project/architecture/02-domain-and-data/database_erd.md)
- [Secuencia de Resolución de Parámetros y Hot-Reload](../project/architecture/04-behavior-and-flows/sequence_parameter_resolution_and_hot_reload.md)
