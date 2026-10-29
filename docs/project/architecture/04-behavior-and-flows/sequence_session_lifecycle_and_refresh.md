---
title: "Diagrama de Secuencia: Ciclo de Vida de Sesiones, Silent Refresh y Timeout (PB-027)"
category: "architecture"
level: "Sequence"
module: "core.security"
tags:
  - sequence
  - security
  - jwt
  - silent-refresh
  - idle-timeout
  - sdd
last_updated: "2026-10-28"
status: "verified"
---

# 🔄 Diagramas de Secuencia: Ciclo de Vida de Sesiones, Silent Refresh y Timeout (PB-027)

Este documento especifica la orquestación temporal de la renovación transparente de tokens (*Silent Refresh*), la rotación criptográfica en base de datos con detección de reúso y el control de inactividad del usuario según lo definido en [ADR-0011](../../../adr/0011_session_lifecycle_and_silent_refresh.md).

---

## 1. Secuencia 1: Silent Refresh Reactivo en Interceptor Angular (Encolamiento ante HTTP 401)

Muestra cómo el `authInterceptor` captura un token expirado, congela las peticiones concurrentes del cajero, renueva el par de tokens y reintenta la operación original sin perder datos:

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as 👤 Cajero (POS)
    participant Componente as 🖥️ PosComponent (Angular)
    participant Interceptor as 🛡️ AuthInterceptor
    participant RefreshQueue as 🚦 RefreshTokenQueue (RxJS)
    participant Backend as ☕ Backend REST (/api/v1)
    participant AuthService as 🔐 AuthService

    Cajero->>Componente: Presiona "Cobrar Comanda"
    Componente->>Interceptor: HTTP POST /api/v1/pos/orders (Token Expirado)
    Interceptor->>Backend: Authorization: Bearer <Access Token Vencido>
    Backend-->>Interceptor: HTTP 401 Unauthorized (TokenExpiredException)
    
    rect rgb(240, 248, 255)
        note over Interceptor, RefreshQueue: Intercepta 401 y activa bloqueo temporal
        Interceptor->>RefreshQueue: isRefreshing = true
        Interceptor->>RefreshQueue: Encola petición original (POST /orders)
        
        par Petición concurrente de alertas mientras refresca
            Componente->>Interceptor: HTTP GET /api/v1/finance/shifts/active
            Interceptor->>RefreshQueue: Encola petición concurrente (GET /shifts)
        end

        Interceptor->>AuthService: executeSilentRefresh(refreshToken)
        AuthService->>Backend: POST /api/v1/auth/refresh { refreshToken }
        Backend-->>AuthService: HTTP 200 OK { newAccessToken, newRefreshToken }
        AuthService-->>Interceptor: Sesión actualizada en memoria
        Interceptor->>RefreshQueue: isRefreshing = false, emit(newAccessToken)
    end

    rect rgb(245, 255, 245)
        note over Interceptor, Backend: Reintento transparente con nuevo token
        RefreshQueue->>Backend: HTTP POST /api/v1/pos/orders (Bearer <Nuevo Access Token>)
        RefreshQueue->>Backend: HTTP GET /api/v1/finance/shifts/active (Bearer <Nuevo Access Token>)
        Backend-->>Componente: HTTP 201 Created (Ticket #104 Pagado)
        Backend-->>Componente: HTTP 200 OK (Turno Activo)
    end

    Componente-->>Cajero: "¡Cobro exitoso! Imprimiendo comanda..."
```

---

## 2. Secuencia 2: Rotación Atómica de Refresh Token y Detección de Reúso (Compromise Detection)

Detalla la validación del hash SHA-256 en base de datos, la revocación inmediata de la fila previa y la expulsión automática de todas las sesiones en caso de intento de reutilización de token:

```mermaid
sequenceDiagram
    autonumber
    participant Cliente as 📱 Cliente REST / POS
    participant Controller as 🌐 AuthController (/refresh)
    participant Service as 🔐 AuthService & JwtService
    participant BD as 🗄️ PostgreSQL (refresh_tokens)
    participant Audit as 📜 AuditLogService

    Cliente->>Controller: POST /api/v1/auth/refresh { "refreshToken": "rt_xyz..." }
    Controller->>Service: refreshSession(refreshToken, clientIp, userAgent)
    Service->>Service: hash = SHA256(refreshToken)
    Service->>BD: SELECT * FROM refresh_tokens WHERE token_hash = :hash

    alt Token no existe o expiró por fecha (expires_at < NOW())
        BD-->>Service: NULL o fecha caducada
        Service-->>Controller: throw SessionExpiredException
        Controller-->>Cliente: HTTP 401 Unauthorized (ProblemDetail: Sesión caducada)
    
    else Token ya revocado (revoked_at IS NOT NULL) -> ATAQUE DE REÚSO DETECTADO
        BD-->>Service: Fila existente pero revoked_at = 2026-10-28 14:00:00
        rect rgb(255, 235, 235)
            note over Service, Audit: Detección de Compromiso: Invalidar TODA la familia
            Service->>BD: UPDATE refresh_tokens SET revoked_at = NOW()<br/>WHERE user_id = :userId AND revoked_at IS NULL
            Service->>Audit: logCriticalSecurityEvent("TOKEN_REUSE_DETECTED", userId, clientIp)
            Service-->>Controller: throw SuspiciousTokenReuseException
            Controller-->>Cliente: HTTP 401 Unauthorized<br/>(ProblemDetail: "Posible brecha de seguridad. Todas las sesiones han sido revocadas")
        end

    else Token Válido y Activo (revoked_at IS NULL) -> ROTACIÓN EXITOSA
        BD-->>Service: Fila activa válida
        rect rgb(235, 255, 235)
            Service->>Service: Genera newAccessToken (15 min) & newRefreshToken (64 bytes)
            Service->>Service: newHash = SHA256(newRefreshToken)
            Service->>BD: INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at)<br/>RETURNING id (nuevo_id)
            Service->>BD: UPDATE refresh_tokens SET revoked_at = NOW(), replaced_by_id = :nuevo_id<br/>WHERE id = :tokenActualId
            Service-->>Controller: RefreshResponse(newAccessToken, newRefreshToken, expiresIn: 900)
            Controller-->>Cliente: HTTP 200 OK { newAccessToken, newRefreshToken, expiresIn: 900 }
        end
    end
```

---

## 3. Secuencia 3: Control de Inactividad en Frontend, Modal de Advertencia y Cierre Automático

Modela el comportamiento reactivo ante el abandono temporal de una terminal POS o consola administrativa:

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as 👤 Usuario / Cajero
    participant DOM as 🖱️ Eventos DOM (Mouse / Teclado)
    participant IdleService as ⏱️ IdleTimerService (Angular)
    participant Modal as ⚠️ InactivityModalComponent
    participant AuthService as 🔐 AuthService
    participant Backend as ☕ Backend REST (/logout)

    Note over IdleService: Inicia monitoreo con timeout = 30 min (alerta a los 28 min)
    
    loop Durante el trabajo activo
        Usuario->>DOM: Movimiento de mouse o tipeo de comanda
        DOM->>IdleService: Evento capturado (debounce 1s)
        IdleService->>IdleService: Reinicia temporizador a 0:00
    end

    Note over Usuario, IdleService: Usuario se retira a la cocina o abandona el mostrador
    IdleService->>IdleService: Transcurren 28 minutos sin eventos DOM

    rect rgb(255, 250, 230)
        note over IdleService, Modal: Ventana de advertencia de 120 segundos
        IdleService->>Modal: openModal(countdownSeconds: 120)
        Modal-->>Usuario: Despliega modal: "¿Sigues ahí? La sesión expirará en 120s"
        
        alt Usuario regresa a tiempo y hace clic en "Continuar"
            Usuario->>Modal: Clic en "Continuar trabajando"
            Modal->>IdleService: resetTimer()
            IdleService->>Modal: closeModal()
            IdleService->>AuthService: touchSession() (Extiende sesión)
        else Pasan los 120 segundos sin interacción
            IdleService->>Modal: countdown reaches 0
            Modal->>AuthService: logoutDueToInactivity()
            AuthService->>Backend: POST /api/v1/auth/logout { refreshToken }
            Backend-->>AuthService: HTTP 200 OK (Revocado)
            AuthService->>AuthService: clearLocalStorageAndSignals()
            AuthService-->>Usuario: Redirecciona a /auth/login<br/>("Sesión cerrada por inactividad")
        end
    end
```
