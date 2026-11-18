---
name: spring-modulith-patterns
description: >-
  Patrones arquitectónicos y reglas inviolables para el desarrollo de módulos en Spring Modulith con Java 25.
  Usar al crear o modificar código en el backend.
---

# Patrones para Spring Modulith en este Proyecto

## 1. Topología de Módulos (`com.restaurant.app`)
- **Núcleo transversal (`core.*`):**
  - `core.common`: Entidades base (`BaseTenantEntity`, `BaseBranchEntity`, `Tenant`, `Branch`), repositorios núcleo (`TenantRepository`, `BranchRepository`), `GlobalExceptionHandler`, `WebConfig`, `DataInitializer`, `AsyncLoggingConfiguration` y excepciones de dominio tipadas en `core.common.exception.*` (`DomainException`, `BusinessRuleException`, `EntityNotFoundException`, `ResourceConflictException`, `ValidationException`, `UnauthorizedOperationException`, `InsufficientStockException`, `ItemSoldOutException`, `ShiftRequiredException`, `PayloadTooLargeException`).
  - `core.config`: `FlywayConfig`, entidades/repositorios de settings (`PlatformSetting*`, `TenantSetting*`, `BranchSetting*`) y contratos `.api` (`PlatformSettingsService`, `PlatformSettingDto`, `TenantConfigService`, `BranchConfigService`).
  - `core.entitlements`: Puerto SPI y caché O(1) de suscripciones/cuotas vía `.api` (`TenantEntitlementService`, `TenantEntitlementPort`, `TenantEntitlementView`, `TenantEntitlementSnapshot`, `TenantEntitlementChangedEvent`, `EntitlementBlockedException`, `QuotaResource`, `SubscriptionStatus`, `SuspensionMode`).
  - `core.security`: Contratos `.api` (`UserPublicApi`, `RolePublicApi`, `SessionPublicApi`, `PasswordChangePolicyPort`, DTOs de solicitud/resumen) e infraestructura JWT/RBAC (`SecurityConfig`, `JwtService`, `JwtCodec`, `JwtSigningSpec`, `JwtAuthenticationFilter`, `TenantContext`, `TenantContextHolder`, `UserDto`, `LoginRequest`, `LoginResponse`, `SystemPermissions`, `AuthController`, `AuthService`, `RolePermissionService`, `AppUser*`, `Role*`, `RefreshToken*`, `UserBranch*`).
  - `core.storage`: Contrato `.api` agnóstico Local/S3 (`MediaStorageService`, `StorageConnectionTestResult`, `StorageProviderType`).
  - `core.audit`: Paquete reservado (`package-info.java`); Backoffice implementa auditoría activa propia en `modules.backoffice` (`platform_audit_logs`).
- **Módulos de Negocio y Plataforma (`modules.*`):**
  - `modules.company`, `modules.configuration`, `modules.catalog`, `modules.inventory`, `modules.pos`, `modules.finance`, `modules.backoffice` (plano SaaS aislado), y `modules.analytics` (stub pendiente de backlog `PB-024`).

## 2. Reglas de Paquetes y Clases `public` en `core.*`
- En cada `modules.<modulo>/`:
  - **`.api`:** Único paquete público expuesto a otros módulos (interfaces públicas, eventos y records/DTOs inmutables).
  - **`.internal`:** Implementaciones, entidades JPA, repositorios y controladores package-private.
- **Uso correcto de clases `public` en `core.*`:** Aunque varias clases de infraestructura y repositorios en `core.common`, `core.config` y `core.security` tienen visibilidad `public`, los módulos `modules.*` **deben consumir `core` exclusivamente a través de sus contratos `.api`**, `TenantContextHolder` y excepciones/superclases comunes. Queda **prohibido** inyectar `AppUserRepository`, `BranchRepository`, `TenantRepository`, `RoleRepository`, etc. directamente en `modules.*`.
- **Inversión de Dependencias (`core.entitlements` $\leftrightarrow$ `modules.backoffice`):** `modules.backoffice` consume `.api` de módulos ERP, por lo que los módulos ERP nunca dependen de `modules.backoffice`; consumen `TenantEntitlementService` (`core.entitlements.api`), cuya implementación SPI (`TenantEntitlementPortImpl`) vive en `modules.backoffice.internal`.
- **Prohibido:** Importar repositorios de otro módulo, crear ciclos de dependencias o mutar tablas de otro módulo sin pasar por `.api` o eventos.

## 3. Eventos Asíncronos Desacoplados y Migraciones Flyway
- Los eventos de dominio son `record`s inmutables en `.api` (`OrderPaidEvent`, `OrderCancelledEvent`, `TenantOnboardedEvent`, `ParameterUpdatedEvent`, `TenantEntitlementChangedEvent`).
- Los consumidores asíncronos utilizan `@ApplicationModuleListener` respaldado por `event_publication` / `event_publication_archive` (`V2`). En `modules.backoffice`, `BackofficeTelemetryListener` combina `@EventListener` síncrono (consumo ACID de gracia) con `@ApplicationModuleListener` (telemetría CQRS).
- **Flyway Dinámico:** Asignar siempre `V(N+1)` secuencial según la última migración aplicada (actual: **`V24`**). Nunca modificar migraciones existentes.

## 4. Verificación de Arquitectura
Toda suite backend debe validar la integridad modular ejecutando `RestaurantModularityTests`:
```java
@Test
void verifyModularity() {
    ApplicationModules.of(RestaurantApplication.class).verify();
}
```
