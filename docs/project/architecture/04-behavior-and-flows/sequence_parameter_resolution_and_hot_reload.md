---
title: "Diagrama de Secuencia: Resolución en Cascada de Parámetros, Hot-Reload y Mutación Atómica"
category: "architecture"
level: "Sequence"
module: "configuration"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - configuration
  - hot-reload
  - sparse-storage
  - caching
last_updated: "2026-10-28"
status: "verified"
---

# ⚙️ Diagrama de Secuencia: Resolución en Cascada de Parámetros, Hot-Reload y Mutación Atómica

Este documento modela la dinámica transaccional del **Sistema de Parametrización Multinivel** ([ADR-0008](../../../adr/0008_unified_multilevel_parameter_system.md) / `TASK-311`): el ciclo de vida de resolución en cascada ($O(1)$) mediante almacenamiento disperso (*Sparse Storage*), la invalidación concurrente de caché en memoria (*Hot-Reload*) y el protocolo de comunicación de alto rendimiento entre Angular 22 Zoneless y Spring Boot 4.1.x.

---

## 🧭 1. Resumen Ejecutivo del Flujo

1. **Resolución Jerárquica en Cascada ($O(1)$):** Cuando cualquier módulo (`POS`, `INVENTORY`, `FINANCE`, `CATALOG`) requiere el valor de una configuración para una sucursal, consulta `ConfigurationPublicApi`. El motor evalúa:
   $$\text{Caché en Memoria} \longrightarrow \text{Sucursal (Branch)} \longrightarrow \text{Empresa (Tenant)} \longrightarrow \text{Global} \longrightarrow \text{Default del Código}$$
2. **Mutación Atómica y Desalojo de Caché:** Ante un cambio desde la UI, se envía un micro-payload `PATCH /api/v1/parameters/{key}` (< 150 bytes), se persiste en `system_parameter_values`, se invalida la clave atómicamente en la caché Caffeine/ConcurrentHashMap y se emite `ParameterUpdatedEvent`.
3. **Comunicación Eficiente en Frontend:** Carga diferida por categorías (`category=POS`), almacenamiento en `Signals` reactivos ($0$ ms al conmutar pestañas) y actualización optimista de interfaz.

---

## ⚡ 2. Secuencia 1: Consulta y Resolución Jerárquica en Tiempo de Ejecución (Pull Model)

Muestra cómo un servicio de negocio (ej. `KardexService` ante una venta) obtiene un parámetro tipado sin penalizar a la base de datos con lecturas repetitivas:

```mermaid
sequenceDiagram
    autonumber
    participant BizService as KardexService (modules.inventory)
    participant ConfigApi as ConfigurationPublicApi (modules.configuration.api)
    participant Cache as ParameterCacheService (internal)
    participant Resolver as CascadingParameterResolver (internal)
    participant DB as PostgreSQL (system_parameter_values)
    participant Catalog as SystemParameterCatalog (internal)

    Note over BizService,ConfigApi: Venta cerrada en POS: Requiere modo de deducción
    BizService->>ConfigApi: getEnum(INVENTORY_DEDUCTION_MODE, branchId, tenantId, DeductionMode.class)
    ConfigApi->>Cache: get(tenantId, branchId, "inventory.deduction_mode")
    
    alt Acierto en Caché en Memoria (Cache Hit - 99% de lecturas)
        Cache-->>ConfigApi: Optional.of("AUTOMATIC_PERMISSIVE")
        ConfigApi-->>BizService: DeductionMode.AUTOMATIC_PERMISSIVE
    else Fallo en Caché (Cache Miss - Primera lectura o post-invalidación)
        Cache->>Resolver: resolve("inventory.deduction_mode", branchId, tenantId)
        
        Note over Resolver,DB: 1. Búsqueda de override de Sucursal (Sparse Storage)
        Resolver->>DB: SELECT parameter_value FROM system_parameter_values WHERE parameter_key = ? AND scope = 'BRANCH' AND branch_id = ?
        alt Existe fila para Sucursal
            DB-->>Resolver: "MANUAL_ISOLATED"
            Resolver-->>Cache: Guarda en caché ("MANUAL_ISOLATED")
            Cache-->>ConfigApi: "MANUAL_ISOLATED"
            ConfigApi-->>BizService: DeductionMode.MANUAL_ISOLATED
        else No existe para Sucursal (Hereda)
            Note over Resolver,DB: 2. Búsqueda de override de Empresa
            Resolver->>DB: SELECT parameter_value FROM system_parameter_values WHERE parameter_key = ? AND scope = 'TENANT' AND tenant_id = ?
            alt Existe fila para Empresa
                DB-->>Resolver: "AUTOMATIC_PERMISSIVE"
                Resolver-->>Cache: Guarda en caché ("AUTOMATIC_PERMISSIVE")
                Cache-->>ConfigApi: "AUTOMATIC_PERMISSIVE"
                ConfigApi-->>BizService: DeductionMode.AUTOMATIC_PERMISSIVE
            else No existe para Empresa (Hereda)
                Note over Resolver,Catalog: 3. Fallback a Plantilla Maestra en Código
                Resolver->>Catalog: getDefaultValue("inventory.deduction_mode")
                Catalog-->>Resolver: "AUTOMATIC_PERMISSIVE"
                Resolver-->>Cache: Guarda en caché ("AUTOMATIC_PERMISSIVE")
                Cache-->>ConfigApi: "AUTOMATIC_PERMISSIVE"
                ConfigApi-->>BizService: DeductionMode.AUTOMATIC_PERMISSIVE
            end
        end
    end
```

---

## 🔄 3. Secuencia 2: Mutación Atómica, Hot-Reload y Notificación Desacoplada

Muestra qué ocurre cuando un Administrador cambia un parámetro desde el navegador:

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador
    participant UI as Frontend Angular (/settings/parameters)
    participant REST as ConfigurationController
    participant Service as ConfigurationServiceImpl
    participant Catalog as SystemParameterCatalog
    participant DB as PostgreSQL (system_parameter_values)
    participant Cache as ParameterCacheService
    participant Events as ApplicationEventPublisher (Spring Modulith)
    participant Listener as LongRunningListener (@ApplicationModuleListener)

    Admin->>UI: Modifica switch "pos.display_dish_images" = false
    UI->>UI: Actualización Optimista en Signal (UI refleja cambio en 0 ms)
    UI->>REST: PATCH /api/v1/parameters/pos.display_dish_images<br/>Payload: {"scope": "BRANCH", "branchId": "...", "value": "false"}
    
    REST->>Service: updateParameter(key, scope, tenantId, branchId, value, userId, reason)
    Service->>Catalog: getDefinition("pos.display_dish_images")
    Catalog-->>Service: ParameterDefinition (type=BOOLEAN, scopeLevel=BRANCH_OVERRIDABLE)
    
    Service->>Service: Valida valor ("false" es booleano válido)
    
    Service->>DB: UPSERT INTO system_parameter_values (parameter_key, scope, tenant_id, branch_id, parameter_value, updated_by, audit_reason)
    DB-->>Service: Fila guardada exitosamente
    
    Service->>Cache: evict(tenantId, branchId, "pos.display_dish_images")
    Note over Cache: Clave eliminada de memoria. Próxima lectura refresca valor.
    
    Service->>Events: publishEvent(new ParameterUpdatedEvent(key, scope, tenantId, branchId, oldValue, newValue))
    Service-->>REST: ParameterDto actualizado
    REST-->>UI: HTTP 200 OK
    
    par Notificación Desacoplada Asíncrona
        Events->>Listener: onParameterUpdated(event)
        Listener->>Listener: Sincroniza estado de componentes de larga duración (si aplica)
    end
```

---

## 🚀 4. Secuencia 3: Estrategia de Comunicación Frontend-Backend de Alto Rendimiento

Demuestra cómo se mitigan los tiempos de espera altos y bloqueos de renderizado mediante *Categorized Lazy Loading* y *ETags*:

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuario Administrador
    participant UI as ParametersPageComponent
    participant Store as ParameterStore (Angular Signals)
    participant HTTP as HttpClient (Angular Interceptor)
    participant API as ConfigurationController (Spring Boot)

    User->>UI: Navega a /settings/parameters
    UI->>Store: loadParameters(scope='BRANCH', category='POS')
    
    alt Categoría ya en memoria de Signals
        Store-->>UI: Retorna Signal reactivo de parámetros POS (0 ms de espera)
    else Primera carga de la categoría
        Store->>UI: Emite Signal con estado isLoading=true (Muestra Skeleton Loader)
        Store->>HTTP: GET /api/v1/parameters?scope=BRANCH&category=POS
        Note over HTTP,API: Envía If-None-Match con ETag previo (si existe)
        
        alt Servidor detecta que no hay cambios (ETag coincide)
            API-->>HTTP: HTTP 304 Not Modified (0 bytes transferidos)
            Store-->>UI: Reutiliza datos en caché (Skeleton desaparece en 15 ms)
        else Datos nuevos o sin ETag
            API->>API: Ejecuta Algoritmo de Fusión (Metadata Catalog + Sparse Storage)
            API-->>HTTP: HTTP 200 OK + ETag Header + JSON comprimido Gzip
            HTTP-->>Store: Lista completa enriquecida de parámetros POS
            Store->>Store: cacheCategory('POS', items)
            Store-->>UI: Actualiza Signal (Renderizado suave sin Cumulative Layout Shift)
        end
    end
```

---

## 🔗 5. Relación con Otros Componentes de Arquitectura

- **Modelo de Persistencia:** [database_erd.md](../02-domain-and-data/database_erd.md) (Tabla `system_parameter_values`).
- **Límites Modulares:** [c4_level3_backend_modulith.md](../01-c4-system/c4_level3_backend_modulith.md) (Módulo `modules.configuration`).
- **Decisión Estructural:** [ADR-0008](../../../adr/0008_unified_multilevel_parameter_system.md).
- **Proceso Operativo de Negocio:** [01_flujo_parametrizacion_operativa_empresa.md](../../../business-processes/04-administracion-y-personal/01_flujo_parametrizacion_operativa_empresa.md).
