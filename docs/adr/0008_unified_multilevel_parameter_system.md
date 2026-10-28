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
last_updated: "2026-10-28"
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
- Requiere mantener el `SystemParameterCatalog` en Java sincronizado con las necesidades de los módulos.
- Exige que los clientes offline (Dexie.js en POS) sincronicen los parámetros de sucursal durante la apertura de turno de caja.
