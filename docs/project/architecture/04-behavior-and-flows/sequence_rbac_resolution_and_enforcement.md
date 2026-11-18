---
title: "Flujo de Resolución RBAC, Vigencia Temporal, Perfil y Cumplimiento en Endpoints"
category: "behavior-flow"
module: "security"
tags: ["sequence", "rbac", "security", "permissions", "must-change-password", "profile", "pin", "multi-branch"]
last_updated: "2026-11-17"
status: "verified"
---

# Flujo de Resolución RBAC, Vigencia Temporal, Perfil y Cumplimiento en Endpoints

Secuencia de resolución de roles y permisos granulares (`recurso:acción`) en tiempo de login/refresh, asignación multi-sucursal (`user_branches`), caducidad temporal (`role_valid_until`), reseteo delegado de contraseña hacia `/profile` y autoservicio de perfil/PIN supervisor (`ProfileController`).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin / Gerente
    actor Usuario as Colaborador / Supervisor
    participant Angular as Guards & Views (Angular)
    participant UserCtrl as UserController (/api/v1/users)
    participant ProfileCtrl as ProfileController (/api/v1/profile)
    participant RoleSvc as RoleManagementServiceImpl / SessionServiceImpl
    participant PolicyPort as SecurityPasswordPolicyAdapter (Configuration)
    participant FinCtrl as FinanceController (/api/v1/finance)

    %% 1. Asignación de Rol, Sucursales y Reseteo Delegado
    Admin->>UserCtrl: POST/PUT /api/v1/users (roleId, roleValidUntil, branchIds)
    UserCtrl->>RoleSvc: Persistir app_users.role_id + user_branches[]

    opt Reseteo delegado de contraseña por supervisor
        Admin->>UserCtrl: POST /api/v1/users/{id}/reset-password (newPassword)
        UserCtrl->>RoleSvc: resetUserPassword(callerId, targetId, request)
        RoleSvc->>PolicyPort: isPasswordChangeEnforcedOnReset(tenantId)
        PolicyPort-->>RoleSvc: Leer security.enforce_password_change_on_reset (default true)
        RoleSvc->>RoleSvc: Actualizar passwordHash y flag must_change_password = enforced
        UserCtrl->>RoleSvc: sessionPublicApi.revokeAllUserSessions(id)
    end

    %% 2. Login y Redirección Obligatoria a /profile
    Usuario->>Angular: Iniciar sesión (POST /api/v1/auth/login)
    Angular->>Angular: Verificar authUser.mustChangePassword
    alt mustChangePassword == true
        Angular->>Angular: LoginComponent + authGuard bloquean rutas de negocio y redirigen a /profile
        Usuario->>ProfileCtrl: PUT /api/v1/profile/password (o POST /api/v1/profile/change-password)
        ProfileCtrl->>ProfileCtrl: Cambiar clave y limpiar must_change_password = false
        Angular-->>Usuario: Desbloquear navegación al sistema (/pos o /company)
    else Vigencia temporal expirada (roleValidUntil < now)
        Angular-->>Usuario: Rechazar login / refresh con HTTP 403 UnauthorizedOperationException
    end

    %% 3. Autoservicio de Perfil y PIN de Supervisor (V12 / V16)
    opt Actualización de contacto o PIN BCrypt propio
        Usuario->>ProfileCtrl: PUT /api/v1/profile (fullName, phoneNumber, email)
        Usuario->>ProfileCtrl: PUT o POST /api/v1/profile/pin (pin 4-6 dígitos -> pin_hash BCrypt)
        ProfileCtrl-->>Usuario: HTTP 200 OK
    end

    %% 4. Verificación en Frontend y Endpoints Protegidos
    Usuario->>Angular: Navegar a Finanzas / Aprobar gasto
    Angular->>Angular: permissionGuard + *hasPrivilege verifican claims permissions[] del JWT
    Angular->>FinCtrl: POST /api/v1/finance/expenses/{expenseId}/approve (Bearer JWT + X-Branch-Id)
    FinCtrl->>FinCtrl: JwtAuthenticationFilter extrae TenantContextHolder (authorities)
    FinCtrl->>FinCtrl: @PreAuthorize / ExpenseService valida finance:expenses_approve
    alt Sin autoridad requerida
        FinCtrl-->>Angular: HTTP 403 Forbidden
    else Autorizado
        FinCtrl-->>Angular: HTTP 200 OK (ExpenseDto APPROVED)
    end
```

## Reglas RBAC y Autoservicio Verificadas

1. **Bloqueo y Redirección por `mustChangePassword` (`auth.guard.ts` & `LoginComponent`):**
   - El backend consulta `security.enforce_password_change_on_reset` mediante el puerto `PasswordChangePolicyPort` (`SecurityPasswordPolicyAdapter`) al ejecutar `POST /api/v1/users/{id}/reset-password` y revoca inmediatamente todas las sesiones previas del colaborador.
   - Si `mustChangePassword == true`, `LoginComponent` redirige directamente a **`/profile`** y todos los guards funcionales (`authGuard`, `guestGuard`, `roleGuard`, `permissionGuard`) impiden acceder a cualquier ruta operativa distinta de `/profile` hasta completar el cambio de contraseña.
2. **Catálogo Real de Permisos (`SystemPermissions` & `RoleController`):**
   - Formato `recurso:acción` verificado en controladores: `pos:access`, `pos:void_item`, `finance:shifts`, `finance:shifts_supervisor`, `finance:expenses_create`, `finance:expenses_approve`, `finance:reports`, `reports:view`, `catalog:read`, `catalog:write`, `inventory:read`, `company:read`, `company:write`, `users:read`, `users:write`, `roles:manage`, `settings:read`, `settings:write`.
   - Roles de sistema (`ADMIN_TENANT`, `SUPERADMIN`) poseen resolución completa de privilegios en su tenant.
3. **Autoservicio de Perfil y PIN (`ProfileController` `/api/v1/profile`):**
   - `GET /api/v1/profile` / `PUT /api/v1/profile`: actualización de `fullName`, `email` y `phoneNumber` (`V16`).
   - `PUT /api/v1/profile/pin` (alias `POST /api/v1/profile/pin`): registra/actualiza el PIN numérico hasheado (`app_users.pin_hash`) usado para autorizaciones presenciales en caja (`V12`).

## Enlaces Relacionados

- [Ciclo de Vida de Sesión y Refresh](./sequence_session_lifecycle_and_refresh.md)
- [Estados de Turnos de Caja y Gastos](./state_shift_and_expense_workflow.md)
- [Anulación de Pedidos y Autorizaciones](./sequence_pos_order_cancellation_and_authorizations.md)
- [Casos de Uso RBAC](../03-use-cases/use_cases_rbac.md)
- [ADR-0010: Control de Acceso RBAC Granular](../../../adr/0010_granular_rbac_roles_and_privileges.md)
- [ADR-0024: Perfil de Usuario Autoservicio y PIN](../../../adr/0024_self_service_user_profile_pin_and_humanization.md)
