---
title: "Secuencia de Onboarding de Nuevo Tenant y Aprovisionamiento"
category: "behavior-flow"
module: "company"
tags: ["sequence", "onboarding", "wizard", "tenant", "presets", "slug", "bolivia", "backoffice"]
last_updated: "2026-11-17"
status: "verified"
---

# Secuencia de Onboarding de Nuevo Tenant y Aprovisionamiento

Flujo del asistente público de **3 pasos** (`OnboardingComponent`) para el registro autónomo de nuevas empresas gastronómicas (`POST /api/v1/auth/onboarding`) y su contraparte administrativa desde Backoffice (`POST /api/v1/backoffice/tenants`), alineado con `OnboardingServiceImpl`.

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Propietario as Propietario / Operador Backoffice
    participant Wizard as OnboardingComponent (Wizard 3 Pasos)
    participant OnbCtrl as OnboardingController
    participant OnbSvc as OnboardingServiceImpl
    participant ConfigApi as ConfigurationPublicApi
    participant UserApi as UserPublicApi + SessionPublicApi
    participant Outbox as Spring EventPublisher

    %% Paso 1: Empresa y Administrador
    Propietario->>Wizard: Paso 1 - Datos legales (legalName, slug, nitOrTaxId opcional) y admin (fullName, username, email, password)
    Wizard->>OnbCtrl: GET /api/v1/auth/check-slug?slug=... (o /api/v1/auth/onboarding/check-availability)
    OnbCtrl-->>Wizard: available + suggestedSlug si existe colisión

    %% Paso 2: Sucursal y Coordenadas
    Propietario->>Wizard: Paso 2 - Sucursal Principal (name, address, phone, moneda, timezone) y preset ciudad Bolivia (lat/lng)
    Note right of Wizard: Sin subida de logo ni mapa OSM; usa presets de ciudades de Bolivia (La Paz, Santa Cruz, etc.)

    %% Paso 3: Perfil Operativo y Confirmación
    Propietario->>Wizard: Paso 3 - Elegir plantilla operativa (FAST_FOOD, TRADITIONAL, CAFE o EMPTY) + includeDemoCatalog

    alt Registro Autónomo Público
        Wizard->>OnbCtrl: POST /api/v1/auth/onboarding (OnboardingRequest)
        OnbCtrl->>OnbSvc: executeOnboarding(request, recordInitialLogin: true)
    else Aprovisionamiento desde Backoffice
        Propietario->>OnbCtrl: POST /api/v1/backoffice/tenants (OnboardingRequest)
        OnbCtrl->>OnbSvc: executeOnboarding(request, recordInitialLogin: false)
    end

    OnbSvc->>OnbSvc: Validar unicidad de slug y NIT (parcial), crear Tenant y Branch matriz
    OnbSvc->>UserApi: Crear usuario ADMIN_TENANT inicial
    opt recordInitialLogin == true
        OnbSvc->>UserApi: recordLoginTimestamp(adminUserId) -> marca app_users.last_login_at
    end

    OnbSvc->>OnbSvc: Si businessTemplate omitido -> fallback a "EMPTY"
    OnbSvc->>ConfigApi: Guardar presets operativos (COMPANY_CURRENCY, COMPANY_TIMEZONE y parámetros por plantilla)

    opt shouldIncludeDemoCatalog == true y template != "EMPTY"
        OnbSvc->>Outbox: publishEvent(TenantOnboardedEvent(tenantId, branchId, template))
        Note right of Outbox: CatalogDataInitializer puebla categorías, platos y stock demo
    end

    OnbSvc->>UserApi: Generar JWT "Ready to Sell" + RefreshToken inicial
    OnbSvc-->>OnbCtrl: OnboardingResultDto (201 Created)
    OnbCtrl-->>Wizard: Sesión lista para operar -> Product Tour y navegación a /company o /pos
```

## Estructura Real del Wizard y Reglas de Aprovisionamiento

1. **Wizard de 3 Pasos (`OnboardingComponent`):**
   - **Paso 1 (`Empresa y Admin`):** Razón social, generación/edición automática de `slug` verificada por debounce en `/api/v1/auth/check-slug`, NIT opcional (`uq_tenant_nit` parcial en `V6`) y cuenta administradora (`ADMIN_TENANT`). **No incluye carga de logotipo.**
   - **Paso 2 (`Sucursal y Ubicación`):** Datos de la sucursal matriz, moneda (`BOB` default), zona horaria (`America/La_Paz` default) y coordenadas `latitude`/`longitude` seleccionables mediante presets de ciudades de Bolivia (`La Paz`, `Santa Cruz`, `Cochabamba`, `El Alto`, `Tarija`, `Sucre`), **sin mapa externo OpenStreetMap**.
   - **Paso 3 (`Perfil Operativo`):** Selección de plantilla (`FAST_FOOD`, `TRADITIONAL`, `CAFE`, `EMPTY`) y bandera `includeDemoCatalog`, más modal resumen antes de confirmar.
2. **Comportamiento Backend (`OnboardingServiceImpl`):**
   - Cuando `businessTemplate` viene nulo o vacío en la petición API, el servidor aplica **`"EMPTY"`** por defecto y omite `TenantOnboardedEvent` (a diferencia del frontend web donde el botón inicial preselecciona `FAST_FOOD` como sugerencia visual).
   - El endpoint `POST /api/v1/backoffice/tenants` invoca `executeOnboarding(request, false)` para dejar `app_users.last_login_at` en `NULL` hasta que el cliente ingrese por primera vez al ERP (`V20` / `TASK-618`).

## Enlaces Relacionados

- [Ciclo de Vida y Entitlements del Tenant](./state_tenant_lifecycle.md)
- [Casos de Uso de Onboarding](../03-use-cases/use_cases_onboarding.md)
- [Casos de Uso de Empresa y Sucursales](../03-use-cases/use_cases_company.md)
- [ADR-0009: Onboarding de Tenants y Presets Operativos](../../../adr/0009_tenant_onboarding_and_operational_presets.md)
- [ADR-0027: Identidad de Plataforma y Bootstrap Clean Go-Live](../../../adr/0027_platform_identity_and_clean_golive_bootstrap.md)
