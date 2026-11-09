---
title: "Diagrama de Secuencia: Perfil de Usuario Autoservicio, PIN y Humanización de Personal"
category: "architecture"
level: "Sequence"
module: "security"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - security
  - profile
  - rbac
  - sprint-5
last_updated: "2026-11-09"
status: "verified"
---

# 👤 Diagrama de Secuencia: Perfil de Usuario Autoservicio, PIN y Humanización (PB-045 / TASK-509)

Este documento modela las interacciones para la actualización autoservicio de datos de contacto (nombre real, teléfono y correo), configuración de contraseña y PIN de supervisor, y la propagación de identidades humanizadas hacia el sidebar, el header y la administración de personal.

---

## 1. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Colaborador / Administrador
    participant ProfileUI as ProfileComponent (/profile)
    participant AuthService as AuthService (Frontend)
    participant Api as ProfileController (Backend)
    participant UserApi as UserPublicApi / UserService
    participant AppUserRepo as AppUserRepository
    participant DB as PostgreSQL (app_users)

    %% Flujo 1: Autoservicio de Datos de Contacto
    Note over Usuario, DB: Flujo 1: Actualización de Nombre Real y Teléfono
    Usuario->>ProfileUI: Ingresa nuevo Nombre ("Carlos Mendizábal") y Teléfono ("71234567")
    Usuario->>ProfileUI: Clic en "Guardar Datos Personales"
    ProfileUI->>Api: PUT /api/v1/profile (UpdateProfileRequest)
    Api->>UserApi: updateProfile(tenantId, userId, request)
    UserApi->>AppUserRepo: findById(userId)
    AppUserRepo-->>UserApi: AppUser
    UserApi->>UserApi: setFullName("Carlos Mendizábal"), setPhoneNumber("71234567")
    UserApi->>DB: UPDATE app_users SET full_name = ..., phone_number = ...
    DB-->>UserApi: Confirmado
    UserApi-->>Api: UserDto actualizado
    Api-->>ProfileUI: 200 OK (UserDto)
    ProfileUI->>AuthService: Actualiza currentUser signal
    ProfileUI->>Usuario: Feedback de éxito ("Datos actualizados correctamente")

    %% Flujo 2: Configuración de PIN de Supervisor
    Note over Usuario, DB: Flujo 2: Gestión de PIN Rápido (4-6 dígitos)
    Usuario->>ProfileUI: Ingresa nuevo PIN ("1234") en formulario de autorización
    Usuario->>ProfileUI: Clic en "Guardar PIN"
    ProfileUI->>Api: PUT /api/v1/profile/pin ({ pin: "1234" })
    Api->>UserApi: updatePin(tenantId, userId, "1234")
    UserApi->>UserApi: Genera BCrypt hash del PIN
    UserApi->>DB: UPDATE app_users SET pin_hash = :hash WHERE id = :userId
    DB-->>UserApi: Confirmado
    Api-->>ProfileUI: 200 OK ("PIN actualizado exitosamente")
    ProfileUI->>Usuario: Feedback de éxito

    %% Flujo 3: Propagación Visual Humanizada en UI
    Note over Usuario, AuthService: Flujo 3: Despliegue de Nombre Real en Header y Tabla
    AuthService-->>Usuario: Sidebar y Header muestran "Carlos Mendizábal" en negrita
    Usuario->>ProfileUI: Vista de perfil muestra Tarjeta Ejecutiva (Rol y Sucursales legibles)
```

---

## 2. Invariantes del Flujo

1. **Fricción Cero en Contacto:** La edición de nombre completo y teléfono no exige reautenticación con contraseña para maximizar la usabilidad del colaborador.
2. **Blindaje Criptográfico:** El cambio de contraseña exige validación obligatoria contra Argon2id de la clave actual; el PIN de autorización rápida se almacena estrictamente bajo hash BCrypt en `app_users.pin_hash`.
3. **Ergonomía Visual:** La interfaz erradica listas crudas de recursos técnicos en `/profile`, priorizando roles amigables y nombres reales de sucursales autorizadas.
