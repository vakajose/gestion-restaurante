---
title: "Diagrama de Secuencia: Resolución de Permisos RBAC, Multi-Sucursal y Reseteo Delegado"
category: "architecture"
level: "Sequence"
module: "security"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - rbac
  - jwt
  - security
last_updated: "2026-10-28"
status: "verified"
---

# 🛡️ Diagramas de Secuencia: Resolución RBAC, Multi-Sucursal y Reseteo Delegado

Este documento formaliza las interacciones dinámicas para la resolución de permisos, rotación de sucursales y administración jerárquica de contraseñas bajo el estándar **SDD** ([ADR-0010](../../../adr/0010_granular_rbac_roles_and_privileges.md) / `TASK-336`).

---

## 🔐 Secuencia 1: Login, Validación de Vigencia Temporal y Emisión de JWT

Muestra el flujo de autenticación, validación de expiración de rol temporal (`role_valid_until`), compilación de sucursales autorizadas (`user_branches`) y mitigación de *token bloat*:

```mermaid
sequenceDiagram
    autonumber
    actor User as Colaborador (Cajero/Mozo)
    participant UI as Cliente Angular (SPA)
    participant AuthCtrl as AuthController (/api/v1/auth/login)
    participant AuthSvc as AuthService
    participant UserRepo as AppUserRepository
    participant RoleRepo as RolePermissionRepository
    participant BranchRepo as UserBranchRepository
    participant JwtSvc as JwtService

    User->>UI: Ingresa slug ("bella-napoli"), usuario ("carlos") y contraseña
    UI->>AuthCtrl: POST /api/v1/auth/login
    AuthCtrl->>AuthSvc: authenticate(LoginRequest)

    AuthSvc->>UserRepo: findByTenantIdAndUsernameOrEmail(tenantId, "carlos")
    UserRepo-->>AuthSvc: AppUser (role, roleValidUntil, mustChangePassword, passwordHash)

    Note over AuthSvc: 1. Validación de Contraseña (Argon2id)
    AuthSvc-->>AuthSvc: passwordEncoder.matches(rawPassword, hash)

    Note over AuthSvc: 2. Validación de Vigencia Temporal del Rol
    alt roleValidUntil != null AND roleValidUntil < NOW()
        AuthSvc-->>AuthCtrl: throw AccountExpiredException("Rol temporal expirado")
        AuthCtrl-->>UI: HTTP 403 Forbidden (ProblemDetail: Rol temporal expirado)
        UI-->>User: Alerta: "Tu rol temporal ha expirado. Contacta a tu administrador."
    end

    Note over AuthSvc: 3. Resolución de Permisos (Mitigación Token Bloat)
    alt role == 'ADMIN_TENANT' OR role == 'SUPERADMIN'
        AuthSvc-->>AuthSvc: permissions = Set.of("*") (Comodín admin)
    else Rol Personalizado u Operativo
        AuthSvc->>RoleRepo: findPermissionsByTenantIdAndRoleCode(tenantId, role)
        RoleRepo-->>AuthSvc: Set<String> (ej. ["pos:access", "pos:order_create"])
    end

    Note over AuthSvc: 4. Resolución de Sucursales Autorizadas
    AuthSvc->>BranchRepo: findBranchIdsByUserId(userId)
    BranchRepo-->>AuthSvc: List<UUID> [SucursalMatriz, SucursalNorte]

    Note over AuthSvc,JwtSvc: 5. Generación de Token JWT
    AuthSvc->>JwtSvc: generateToken(userId, role, permissions, branchIds, defaultBranchId)
    JwtSvc-->>AuthSvc: JWT Compacto (Claim permissions, branch_ids)

    AuthSvc-->>AuthCtrl: LoginResponse (token, userDto, mustChangePassword)
    AuthCtrl-->>UI: HTTP 200 OK
    UI-->>User: Redirección al POS o Tablero con Sucursal Activa
```

---

## ⚡ Secuencia 2: Aplicación de Seguridad en Backend (@PreAuthorize) y Frontend (*hasPermission)

Ilustra la aplicación de permisos en una operación crítica (ej. Autorización de Gasto Menor en Caja Chica):

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Cajero / Encargado
    participant UI as Vista Angular (/finance/expenses)
    participant PermDirective as Directiva *hasPermission / Signal
    participant Interceptor as HttpBranchInterceptor
    participant Filter as JwtAuthenticationFilter
    participant Controller as ExpenseController
    participant Service as ExpenseService

    Note over UI,PermDirective: Evaluación Reactiva en Frontend (Zoneless Signals)
    UI->>PermDirective: Evalúa *hasPermission="'finance:expenses:approve'"
    PermDirective-->>PermDirective: authService.hasPermission("finance:expenses:approve")
    alt Usuario NO tiene permiso ni comodín "*"
        PermDirective-->>UI: Oculta botón "Autorizar Gasto" en el DOM
    else Usuario TIENE permiso
        PermDirective-->>UI: Renderiza botón activo y habilitado
    end

    Cashier->>UI: Pulsa "Autorizar Gasto (ID: 101)"
    UI->>Interceptor: PATCH /api/v1/expenses/101/approve
    Interceptor->>Interceptor: Inyecta Authorization: Bearer <JWT> y X-Branch-Id: <UUID>
    Interceptor->>Filter: Envía Petición HTTP

    Note over Filter: Validación de Token y Contexto de Sucursal
    Filter->>Filter: extractTenantContext(token)
    Filter->>Filter: Verifica que X-Branch-Id figure en claim "branch_ids" (O(1))
    Filter->>Filter: Puebla SecurityContext con SimpleGrantedAuthority(permisos)
    Filter->>Controller: Invoca Endpoint Protegido

    Note over Controller: Verificación Declarativa Spring Security
    Controller->>Controller: @PreAuthorize("hasAuthority('finance:expenses:approve')")
    Controller->>Service: approveExpense(tenantId, branchId, expenseId)
    Service-->>Controller: Gasto Aprobado (AUDIT_LOG registrado)
    Controller-->>UI: HTTP 200 OK (Gasto Actualizado)
    UI-->>Cashier: Notificación Toast: "Gasto de caja chica autorizado con éxito"
```

---

## 🔑 Secuencia 3: Circuito Jerárquico de Reseteo Asistido de Contraseñas

Muestra cómo se resuelve la pérdida de contraseña para un cajero o cocinero sin correo electrónico corporativo:

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador / Encargado de Sucursal
    actor Cashier as Cajero (Sin Correo)
    participant UI_Admin as Panel Admin (/users)
    participant UserCtrl as UserController (/api/v1/users/{id}/reset-password)
    participant UserSvc as UserService
    participant UserRepo as AppUserRepository
    participant UI_Cashier as Login / POS Cajero

    Cashier->>Admin: "Olvidé mi contraseña de acceso a la caja"
    Admin->>UI_Admin: Abre modal "Resetear Contraseña" del cajero
    UI_Admin->>Admin: Solicita nueva clave temporal o genera sugerencia ("Caja*2026")
    Admin->>UI_Admin: Confirma reseteo con "Caja*2026"
    UI_Admin->>UserCtrl: POST /api/v1/users/{cajeroId}/reset-password

    Note over UserCtrl,UserSvc: Validación de Jerarquía de Reseteo
    UserCtrl->>UserSvc: resetPasswordDelegated(requesterId, targetUserId, newPassword)
    UserSvc->>UserSvc: Valida que Requester sea ADMIN_TENANT o BRANCH_MANAGER autorizado en la sucursal del cajero
    UserSvc->>UserSvc: hash = passwordEncoder.encode("Caja*2026") (Argon2id)
    UserSvc->>UserRepo: updatePasswordAndFlag(targetUserId, hash, mustChangePassword=TRUE)

    UserSvc-->>UserCtrl: Operación Exitosa
    UserCtrl-->>UI_Admin: HTTP 200 OK
    UI_Admin-->>Admin: "Contraseña reseteada. Indique al colaborador su clave temporal."
    Admin->>Cashier: Entrega clave temporal verbalmente ("Caja*2026")

    Note over Cashier,UI_Cashier: Primer Acceso con Clave Temporal
    Cashier->>UI_Cashier: Ingresa "carlos" y "Caja*2026"
    UI_Cashier->>UI_Cashier: Login exitoso, detecta must_change_password == TRUE
    UI_Cashier->>UI_Cashier: Bloquea acceso a rutas operativas y abre modal obligatorio
    UI_Cashier-->>Cashier: "Por seguridad, debes definir tu nueva contraseña personal"
    Cashier->>UI_Cashier: Ingresa nueva clave personal ("MiClaveSegura#1")
    UI_Cashier->>UserCtrl: PUT /api/v1/profile/change-password
    UserCtrl-->>UI_Cashier: HTTP 200 OK (mustChangePassword=FALSE)
    UI_Cashier-->>Cashier: ¡Bienvenido! Redirige al Punto de Venta listo para cobrar
```
