---
title: "ADR 0008: Sistema Unificado de Parametrización Multinivel (Registry en Código y Sparse Storage)"
category: "adr"
tags:
  - adr
  - architecture
  - configuration
  - multi-tenant
  - performance
  - hot-reload
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0008: Sistema Unificado de Parametrización Multinivel (Registry en Código y Sparse Storage)

## Contexto
El sistema POS + ERP para Restaurantes atiende a múltiples tipos de negocios (desde puestos de comida rápida hasta cadenas de franquicias y restaurantes tradicionales a la carta). Para evitar la sobre-especialización del código y soportar requerimientos divergentes de clientes, el comportamiento del sistema debe ser completamente parametrizable.

Anteriormente, [ADR-0002](./0002_hybrid_tenancy_and_dynamic_settings.md) introdujo tablas clave-valor separadas (`tenant_settings` y `branch_settings`). Sin embargo, dicho enfoque presentaba limitaciones estructurales críticas:
1. **Falta de Catálogo Centralizado:** Las opciones disponibles vivían como texto libre en BD. No existía una plantilla maestra para saber qué parámetros existen a menos de poblar miles de registros iniciales para cada nuevo restaurante.
2. **Duplicidad de Tablas y Código:** Mantener tablas independientes para `TENANT` y `BRANCH` obligaba a duplicar entidades JPA, repositorios y servicios de consulta, impidiendo además un nivel `GLOBAL` (Superadmin) sin crear una tercera tabla.
3. **Ausencia de Auditoría y Control de Criticidad:** No se registraba quién (`updated_by`), cuándo ni por qué (`audit_reason`) se modificó una regla sensible (ej. modos de Kardex o límites de caja menor).
4. **Riesgo de Ineficiencia en Red:** Transferir listas masivas de configuración hacia el frontend de forma monolítica degrada la experiencia de usuario (*Cumulative Layout Shift* y tiempos de espera altos).

---

## Decisión

Se adopta una arquitectura de parametrización inspirada en los estándares de **VS Code Settings, Magento Scope Tree y Feature Flagging moderno**, formalizada bajo las siguientes directrices:

### 1. Desacoplamiento entre Metadatos y Persistencia (Sparse Storage)
- **Catálogo de Metadatos en Código (`SystemParameterCatalog`):**  
  Todas las definiciones de parámetros (clave canónica, nombre, descripción, categoría funcional, ámbito permitido, tipo de dato, valor por defecto, opciones enum, criticidad y flag de recarga) residen en código Java 25 (`modules.configuration.internal`). Constituyen la plantilla maestra inmutable entre versiones de software.
- **Almacenamiento Disperso (*Sparse Storage*):**  
  La base de datos **solo almacena filas cuando el usuario sobreescribe explícitamente un valor por defecto**. Una empresa nueva nace con $0$ filas en base de datos y hereda el 100% de su comportamiento del catálogo en código.

### 2. Tabla Relacional Única Unificada (`system_parameter_values`)
Se unifican `tenant_settings` y `branch_settings` en la tabla `system_parameter_values`:
- Columnas: `(id, parameter_key, scope, tenant_id, branch_id, parameter_value, updated_at, updated_by, audit_reason)`.
- Restricción única determinista:  
  `UNIQUE (parameter_key, scope, COALESCE(tenant_id, '000...'), COALESCE(branch_id, '000...'))`.
- Soporta de forma nativa los tres niveles: `GLOBAL`, `TENANT` y `BRANCH`.

### 3. Resolución Jerárquica en Cascada ($O(1)$)
El motor de configuración evalúa en orden de precedencia:
$$\text{Branch Override (BD)} \longrightarrow \text{Tenant Override (BD)} \longrightarrow \text{Global Override (BD)} \longrightarrow \text{Code Default (Registry)}$$
La resolución se respalda en una caché concurrente en memoria (Caffeine / `ConcurrentHashMap`), garantizando latencia sub-microsegundo por consulta.

### 4. Hot-Reload Transparente e Invalidación Concurrente
- El Hot-Reload reside en el motor `modules.configuration`.
- Los módulos consumidores consultan `ConfigurationPublicApi.get(...)` en cada transacción o petición.
- Al mutar un parámetro, la caché local se desaloja atómicamente y la siguiente transacción consume el nuevo valor de inmediato sin reinicio de la aplicación.
- Para componentes reactivos de larga duración, se publica `ParameterUpdatedEvent` desacoplado mediante `@ApplicationModuleListener`.

### 5. Control de Criticidad y Auditoría
- **Criticidad Baja (`LOW`):** Parámetros cosméticos o de UI (`pos.display_dish_images`). Actualización directa sin confirmación.
- **Criticidad Media (`MEDIUM`):** Reglas operativas no destructivas. Guardado con notificación informativa.
- **Criticidad Alta (`HIGH`):** Reglas contables o de inventario (`inventory.deduction_mode`). Exigen reautenticación con contraseña de administrador, advertencia de consecuencias operativas y justificación obligatoria en `audit_reason`.

### 6. Comunicación de Alto Rendimiento con el Frontend
- **Carga Diferida por Categorías (*Categorized Lazy-Loading*):** Consulta segmentada (`category=POS`), cacheada en `Signals` de Angular 22 en cliente ($0$ ms al alternar pestañas).
- **Mutaciones Atómicas de Campo Individual (*Atomic PATCH*):** `PATCH /api/v1/parameters/{key}` (< 150 bytes) con actualización optimista en UI.
- **Eficiencia HTTP:** Encabezados `ETag` y respuestas `HTTP 304 Not Modified`.

---

## Consecuencias

### Positivas
- **Base de Datos Ultraliviana:** Cero registros basura o huérfanos.
- **Despliegues sin Fricción:** Nuevos parámetros aparecen instantáneamente con sus valores por defecto sin migraciones SQL masivas.
- **Cero Código Espagueti:** Los módulos de negocio consumen parámetros mediante el patrón Strategy y métodos semánticos tipados, sin bifurcaciones `if-else` anidadas.
- **UX Instantánea:** El frontend no sufre demoras al cargar ni al guardar configuraciones.

### Negativas / Compensaciones
- Requiere mantener el catálogo de parámetros sincronizado con las necesidades de los módulos.
- Exige que los clientes offline (Dexie.js en POS) sincronicen los parámetros de sucursal durante la apertura de turno de caja.

## Enmienda y Evolución (ADR-0025, ADR-0027 y ADR-0028)
> [!NOTE]
> - **Metadatos externalizados ([ADR-0025](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md)):** Los nombres humanizados, descripciones, casos de uso reales y etiquetas amigables de opciones ya no viven hardcodeados en Java; `SystemParameterCatalog` carga el archivo externo `backend/src/main/resources/config/parameters-catalog.json` (actualmente **20 parámetros operativos** de restaurante).
> - **Segregación de Plataforma ([ADR-0027](./0027_platform_identity_and_clean_golive_bootstrap.md), [ADR-0028](./0028_agnostic_media_storage_and_platform_settings.md)):** El nivel `GLOBAL` para parámetros internos del operador SaaS (`storage.active_provider`, `storage.max_file_size_bytes`) fue segregado hacia la tabla física **`platform_settings`**, dejando `system_parameter_values` exclusivamente para parámetros operativos de restaurantes (`TENANT` / `BRANCH`).

## Documentos Relacionados
- [ADR-0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica](./0002_hybrid_tenancy_and_dynamic_settings.md)
- [ADR-0009: Onboarding Guiado de Empresa y Presets Operativos](./0009_tenant_onboarding_and_operational_presets.md)
- [ADR-0010: Control de Acceso RBAC Dinámico](./0010_granular_rbac_roles_and_privileges.md)
- [ADR-0011: Ciclo de Vida de Sesiones y Silent Refresh](./0011_session_lifecycle_and_silent_refresh.md)
- [ADR-0016: Separación de Control de Caja y Turnos Multi-Cajero](./0016_cash_control_separation_and_multi_cashier_shifts.md)
- [ADR-0020: Modalidades de Servicio y Delivery](./0020_service_modalities_takeout_delivery_and_ticket_badges.md)
- [ADR-0025: Humanización de Parámetros, Catálogo JSON y Ayuda Contextual](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md)
- [ADR-0027: Identidad de Plataforma y Bootstrap Limpio](./0027_platform_identity_and_clean_golive_bootstrap.md)
- [ADR-0028: Almacenamiento Agnóstico de Media y `platform_settings`](./0028_agnostic_media_storage_and_platform_settings.md)
- [ADR-0030: Aislamiento del Catálogo por Sucursal (`catalog.scope_mode`)](./0030_catalog_branch_isolation_by_default.md)
- [Casos de Uso de Onboarding](../project/architecture/03-use-cases/use_cases_onboarding.md)
- [Casos de Uso de Configuración](../project/architecture/03-use-cases/use_cases_configuration.md)
- [Secuencia de Resolución de Parámetros y Hot-Reload](../project/architecture/04-behavior-and-flows/sequence_parameter_resolution_and_hot_reload.md)
