---
title: "Diagrama de Secuencia: Asistente de Onboarding y Transacción Atómica de Alta"
category: "architecture"
level: "Sequence"
module: "company"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - onboarding
  - multi-tenant
  - atomic-transaction
last_updated: "2026-10-28"
status: "verified"
---

# 🚀 Diagrama de Secuencia: Asistente de Onboarding y Transacción Atómica de Alta

Este documento modela la dinámica transaccional del **Proceso de Onboarding Autónomo (*Self-Service*)** ([ADR-0009](../../../adr/0009_tenant_onboarding_and_operational_presets.md) / `TASK-333`): desde la validación reactiva en el cliente Angular hasta la ejecución coordinada y atómica en Spring Boot 4.1.x y PostgreSQL 16.

---

## 🧭 1. Resumen Ejecutivo del Flujo

1. **Interacción Asíncrona Previa:** Mientras el usuario escribe el nombre de su negocio, el frontend infiere el `slug`, consulta la disponibilidad en tiempo real sin recargar página y valida el NIT (si fue introducido).
2. **Transacción Atómica `@Transactional`:** El backend recibe `OnboardingRequestDto`, valida la unicidad, persiste el `Tenant`, la `Branch` matriz con huso horario boliviano predeterminado, el usuario administrador en `app_users` (Argon2id), aplica 2 a 3 parámetros en `system_parameter_values` (Sparse Storage) e inserta opcionalmente los 3 platos semilla demo.
3. **Emisión de Token JWT "Ready to Sell":** Sin requerir un segundo login manual, se emite el token JWT y el contexto de sucursal (`X-Branch-Id`), redirigiendo al usuario al panel de inicio o terminal POS.

---

## ⚡ 2. Secuencia Principal: Registro Atómico de Alta de Empresa

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Nuevo Propietario (Cliente)
    participant UI as OnboardingWizard (Angular 22)
    participant Modal as ResumenPrevioModal
    participant REST as OnboardingController (modules.company)
    participant Svc as OnboardingServiceImpl (internal)
    participant TenantRepo as TenantRepository
    participant BranchRepo as BranchRepository
    participant UserApi as UserPublicApi (core.security.api)
    participant ConfigApi as ConfigurationPublicApi (modules.configuration.api)
    participant CatalogSvc as CatalogDemoSeeder (internal)
    participant JwtSvc as JwtService (core.security)

    Usuario->>UI: Completa los 4 pasos y selecciona Preset Operativo
    Usuario->>UI: Clic en "Continuar a Confirmación"
    UI->>Modal: Despliega Resumen Interactivo de Datos
    Usuario->>Modal: Confirma ("Crear mi Restaurante")
    
    Modal->>REST: POST /api/v1/auth/onboarding (OnboardingRequestDto)
    REST->>Svc: executeOnboarding(dto)
    
    Note over Svc,TenantRepo: Inicia Transacción Única @Transactional
    
    Svc->>Svc: Valida formato de slug y unicidad de NIT (si no es nulo)
    
    Svc->>TenantRepo: save(new Tenant(legalName, nitOrTaxId, slug))
    TenantRepo-->>Svc: Tenant persistido (tenantId, slug)
    
    Svc->>BranchRepo: save(new Branch(tenantId, "Sucursal Principal", "America/La_Paz", address, phone))
    BranchRepo-->>Svc: Branch matriz persistida (branchId)
    
    Svc->>UserApi: createUser(tenantId, new CreateUserRequest(username, fullName, email, password, "ADMIN_TENANT", branchId))
    UserApi-->>Svc: AppUser persistido con hash Argon2id (userId)
    
    alt Preset Operativo Seleccionado (Fast Food / Tradicional / Bar)
        Note over Svc,ConfigApi: Aplica 2 o 3 overrides del perfil (Sparse Storage - ADR-0008)
        Svc->>ConfigApi: applyOperationalPreset(tenantId, branchId, preset, currency)
        ConfigApi-->>Svc: Overrides registrados en system_parameter_values
        
        Note over Svc,CatalogSvc: Inyecta los 3 platos de prueba con recetas simples
        Svc->>CatalogSvc: seedDemoCatalog(tenantId, branchId, preset)
        CatalogSvc-->>Svc: Categorías y Platos demo persistidos
    else Plantilla Vacía Seleccionada
        Note over Svc,ConfigApi: Registra únicamente la moneda base elegida
        Svc->>ConfigApi: setOverride(CURRENCY, TENANT, tenantId, null, currency)
    end
    
    Note over Svc,JwtSvc: Genera credenciales de acceso inmediato
    Svc->>JwtSvc: generateToken(user, tenant.slug, [branchId], ["ADMIN_TENANT"])
    JwtSvc-->>Svc: accessToken
    
    Note over Svc,TenantRepo: Commit exitoso de la transacción
    
    Svc-->>REST: OnboardingResponseDto (tenantId, slug, branchId, accessToken)
    REST-->>Modal: HTTP 201 Created
    Modal-->>UI: Cierra modal y entrega credenciales al AuthService del frontend
    UI->>Usuario: Redirección inmediata a /pos o /dashboard ("¡Listo para operar!")
```

---

## 🔍 3. Secuencia Secundaria: Validación Reactiva de Disponibilidad de Slug y NIT

Evita que el usuario llegue al final del formulario para descubrir que su identificador comercial ya estaba en uso:

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuario
    participant Input as Input Nombre Comercial / Slug
    participant Debounce as RxJS / Signals Debounce (350ms)
    participant API as OnboardingController
    participant Repo as TenantRepository

    Usuario->>Input: Escribe "Parrilladas El Fogón"
    Input->>Input: Infiriere slug: "parrilladas-el-fogon"
    Input->>Debounce: Dispara evento con debounce 350ms
    Debounce->>API: GET /api/v1/auth/check-slug?slug=parrilladas-el-fogon
    API->>Repo: existsBySlug("parrilladas-el-fogon")
    
    alt Slug Disponible
        Repo-->>API: false
        API-->>Input: {"available": true, "suggestedSlug": "parrilladas-el-fogon"}
        Input->>Usuario: Muestra ícono verde de disponibilidad ✔️
    else Slug Ocupado (Colisión)
        Repo-->>API: true
        API->>API: Genera sugerencia con sufijo aleatorio ("parrilladas-el-fogon-k9x2")
        API-->>Input: {"available": false, "suggestedSlug": "parrilladas-el-fogon-k9x2"}
        Input->>Usuario: Muestra sugerencia alternativa seleccionable en 1 clic
    end
```

---

## 🔗 4. Relación con Otros Componentes

- **Decisión Arquitectónica:** [ADR-0009](../../../adr/0009_tenant_onboarding_and_operational_presets.md).
- **Esquema de Base de Datos:** [database_erd.md](../02-domain-and-data/database_erd.md).
- **Proceso Operativo de Negocio:** [02_flujo_alta_empresa_y_onboarding.md](../../../business-processes/04-administracion-y-personal/02_flujo_alta_empresa_y_onboarding.md).
- **Casos de Uso:** [use_cases_onboarding.md](../03-use-cases/use_cases_onboarding.md).
