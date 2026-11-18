---
title: "Ciclo de Vida de Sesión, Silent Refresh con Rotación e Inactividad"
category: "behavior-flow"
module: "security"
tags: ["sequence", "session", "jwt", "refresh-token", "silent-refresh", "reuse-detection", "idle-timer", "adr-0011"]
last_updated: "2026-11-17"
status: "verified"
---

# Ciclo de Vida de Sesión, Silent Refresh con Rotación e Inactividad

Flujo técnico de autenticación JWT + `RefreshToken` opaco con hash SHA-256 (`refresh_tokens`), rotación atómica en cadena (`replaced_by_id`), detección automática de reúso ante robo de tokens, gestión de cola RxJS en `authInterceptor` y monitoreo de inactividad en el navegador (`IdleTimerService`).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuario ERP
    participant UI as App / IdleTimerService + InactivityModal
    participant Int as authInterceptor (Angular)
    participant AuthCtrl as AuthController (/api/v1/auth)
    participant SessSvc as SessionServiceImpl
    participant EntSvc as TenantEntitlementService
    participant DB as PostgreSQL (refresh_tokens)

    %% 1. Inicio de Sesión
    Usuario->>AuthCtrl: POST /api/v1/auth/login (tenantIdentifier, login, password)
    AuthCtrl->>SessSvc: Validar credenciales + assertLoginAllowed(tenantId)
    SessSvc->>DB: Insertar RefreshToken (SHA-256 token_hash, TTL 7 días)
    AuthCtrl-->>Int: accessToken (JWT 900s default) + refreshToken opaco + UserDto
    UI->>UI: IdleTimerService.startWatching() (mousemove, keydown, click, scroll, touchstart)

    %% 2. Renovación Silenciosa ante 401 con Cola RxJS
    UI->>Int: Peticiones paralelas de negocio con JWT expirado
    Int-->>Int: Backend responde HTTP 401 Unauthorized
    Int->>Int: Bloquear nuevas renovaciones (isRefreshing = true) y encolar peticiones en refreshTokenSubject
    Int->>AuthCtrl: POST /api/v1/auth/refresh (refreshToken)
    AuthCtrl->>SessSvc: refreshSession(request, ip, userAgent)
    SessSvc->>DB: Buscar refresh_tokens por token_hash
    SessSvc->>EntSvc: assertLoginAllowed(tenantId) (bloquea si SuspensionMode == TOTAL)

    alt storedToken.revokedAt != null (Detección de Reúso - ADR-0011)
        SessSvc->>DB: revokeAllActiveByUserId(userId, now)
        SessSvc-->>AuthCtrl: BadCredentialsException (HTTP 401)
        AuthCtrl-->>Int: Sesión revocada masivamente
        Int->>UI: authService.logout(false) -> Redirigir a /auth/login
    else Token válido y vigente
        SessSvc->>DB: Emitir nuevo RefreshToken + marcar anterior revoked_at & replaced_by_id
        SessSvc->>SessSvc: Re-resolver permisos RBAC y sucursales actuales -> nuevo JWT
        SessSvc-->>AuthCtrl: RefreshResponse (nuevo accessToken + nuevo refreshToken)
        AuthCtrl-->>Int: HTTP 200 OK
        Int->>Int: Liberar refreshTokenSubject con nuevo JWT y reintentar peticiones encoladas
    end

    %% 3. Monitoreo de Inactividad y Advertencia Preventiva
    Note over UI: Sin interacción del usuario durante (timeoutMinutes * 60 - 120) segundos
    UI->>UI: triggerWarning() -> Abrir InactivityModal (cuenta regresiva 120s)
    alt Usuario presiona "Continuar trabajando"
        Usuario->>UI: continueWorking() -> Reiniciar temporizador
    else Cuenta regresiva llega a 0
        UI->>AuthCtrl: POST /api/v1/auth/logout (refreshToken) -> Revocar sesión
        AuthCtrl-->>UI: Redirigir a /auth/login
    end
```

## Contratos y Comportamiento de Timeout Verificados

1. **Vigencias en Servidor (`SessionServiceImpl`):**
   - `accessToken` JWT: expiración configurable vía `restaurant.security.jwt.expiration-seconds` (default **`900` segundos / 15 minutos**).
   - `refreshToken`: cadena opaca persistida como hash SHA-256 (`refresh_tokens.token_hash`), vigencia de **7 días** (`REFRESH_TOKEN_TTL_DAYS = 7L`).
   - Gestión de sesiones activas del usuario: `GET /api/v1/auth/sessions`, `DELETE /api/v1/auth/sessions/{sessionId}`, `POST /api/v1/auth/logout` y `POST /api/v1/auth/revoke-all`.
2. **Parámetro vs Implementación Actual de Inactividad:**
   - El catálogo de parámetros [`parameters-catalog.json`](../../../../backend/src/main/resources/config/parameters-catalog.json) define **`security.session_inactivity_timeout_minutes`** con valor por defecto **`15`** minutos.
   - En el frontend actual ([`idle-timer.service.ts`](../../../../frontend/src/app/core/services/idle-timer.service.ts)), `IdleTimerService` inicializa por defecto **`DEFAULT_IDLE_TIMEOUT_MINUTES = 30`** minutos (mínimo configurable `3` min en `setTimeoutMinutes`) y **`WARNING_COUNTDOWN_SECONDS = 120`** segundos de aviso previo en `InactivityModal`.
3. **Prioridad ante Bloqueo SaaS:** En `SessionServiceImpl.refreshSession`, `tenantEntitlementService.assertLoginAllowed(tenantId)` se evalúa antes de detectar reúso de token para impedir respuestas `401` ambiguas cuando el tenant se encuentra suspendido en modo `TOTAL`.

## Enlaces Relacionados

- [Resolución RBAC y Autorizaciones](./sequence_rbac_resolution_and_enforcement.md)
- [Ciclo de Vida y Entitlements del Tenant](./state_tenant_lifecycle.md)
- [Casos de Uso de Sesiones](../03-use-cases/use_cases_sessions.md)
- [ADR-0011: Ciclo de Vida de Sesión y Silent Refresh](../../../adr/0011_session_lifecycle_and_silent_refresh.md)
