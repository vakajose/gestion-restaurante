---
title: "Máquina de Estados: Ciclo de Vida de la Empresa (Tenant Lifecycle)"
category: "architecture"
level: "State"
module: "company"
tags:
  - architecture
  - mermaid
  - sdd
  - state-machine
  - tenant
  - lifecycle
last_updated: "2026-10-28"
status: "verified"
---

# 🏢 Máquina de Estados: Ciclo de Vida de la Empresa (Tenant Lifecycle)

Este documento modela las fases, estados operacionales y transiciones del ciclo de vida de una empresa inquilina (**Tenant**) en la plataforma, desde su creación en el asistente de onboarding ([ADR-0009](../../../adr/0009_tenant_onboarding_and_operational_presets.md)) hasta su eventual suspensión o archivado definitivo.

---

## 🗺️ 1. Diagrama de Transición de Estados (Mermaid)

```mermaid
stateDiagram-v2
    [*] --> FormularioOnboarding : Usuario accede a /onboarding

    state FormularioOnboarding {
        [*] --> Paso1_Identidad : Razón social & Slug
        Paso1_Identidad --> Paso2_Sucursal : Datos sucursal matriz
        Paso2_Sucursal --> Paso3_Preset : Selección de plantilla/platos demo
        Paso3_Preset --> Paso4_Admin : Creación de credenciales
        Paso4_Admin --> ModalResumen : Revisión de datos
        ModalResumen --> Paso1_Identidad : Editar datos
    }

    FormularioOnboarding --> Activo : Confirmación & Transacción atómica exitosa
    FormularioOnboarding --> [*] : Abandono de registro (0 datos en BD)

    state Activo {
        [*] --> OperacionNormal : Acceso a POS, Inventario y Finanzas
        OperacionNormal --> OperacionNormal : Creación de sucursales adicionales / Edición
    }

    Activo --> Suspendido : Bloqueo administrativo / Mora (Superadmin)
    Suspendido --> Activo : Reactivación por regularización comercial

    Activo --> Archivado : Baja voluntaria / Cancelación definitiva de cuenta
    Suspendido --> Archivado : Plazo de gracia vencido (Retención 90 días)

    Archivado --> [*] : Purga legal o anonimización de datos
```

---

## 📋 2. Descripción de Estados e Invariantes de Negocio

| Estado | Significado Operativo | Acceso Permitido para Colaboradores | Mutabilidad de Datos |
| :--- | :--- | :--- | :--- |
| **`FormularioOnboarding`** | El usuario se encuentra completando los pasos del asistente visual en el navegador. | No existe sesión ni token JWT emitido. | Solo vive en memoria del cliente. Cero registros en PostgreSQL. |
| **`Activo`** (`is_active = true`) | La empresa está plenamente habilitada en la plataforma con al menos su sucursal matriz y su administrador. | Acceso completo según rol asignado (POS, comandas, arqueos, inventario). | Lectura y escritura transaccional ilimitada. |
| **`Suspendido`** (`is_active = false`) | Bloqueo administrativo preventivo impuesto por el operador SaaS (Superadmin). | Login bloqueado con mensaje explicativo: *"Cuenta temporalmente suspendida. Contacte a soporte"*. | Base de datos en solo lectura técnica; operaciones comerciales bloqueadas. |
| **`Archivado`** | La empresa solicitó la baja del servicio o finalizó su contrato. | Acceso revocado permanentemente para todos los usuarios del tenant. | Datos congelados para cumplimiento fiscal/auditoría histórica. |

---

## 🔗 3. Referencias Relacionadas

- **Transacción Atómica de Alta:** [sequence_tenant_onboarding.md](./sequence_tenant_onboarding.md).
- **Modelo Relacional Físico:** [database_erd.md](../02-domain-and-data/database_erd.md).
- **Decisión Arquitectónica:** [ADR-0009](../../../adr/0009_tenant_onboarding_and_operational_presets.md).
