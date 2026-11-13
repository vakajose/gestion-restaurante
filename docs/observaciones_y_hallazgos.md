---
title: "Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Inbox de Triage)"
category: "feedback"
status: "living-document"
last_updated: "2026-11-13"
tags:
  - observations
  - bugs
  - feedback
  - triage
  - backlog-inbox
---

# 📥 Bitácora Dinámica de Observaciones, Hallazgos y Mejoras

> **Propósito:** Documento dinámico e interactivo para que el usuario o equipo registre libremente observaciones, comportamientos inesperados, bugs, mejoras visuales (UI/UX) o ideas de producto mientras explora o prueba el sistema.  
> **Flujo de Operación:**
> 1. **Registro:** El usuario anota libremente hallazgos siguiendo la plantilla estándar.
> 2. **Estado Inicial:** Todo nuevo registro nace en estado `[PENDIENTE]` (no requiere acción inmediata).
> 3. **Triage y Conversión:** Cuando el usuario lo indique expresamente en el chat, el agente revisará los ítems pendientes y los transformará en tareas formales (`docs/tasks.md` / `Product Backlog`).

---

## 📊 Resumen Rápido (Estado Actual)

| ID | Fecha | Módulo / Pantalla | Tipo | Severidad / Prioridad | Título Breve | Estado |
|:---|:---:|:---|:---:|:---:|:---|:---:|
| *OBS-001* | 2026-11-07 | Parametros del sistema | Bug | Media | *Al ingresar a parametros del sistema y al navegar entre la opcion de empresa y sucursales salta un badge de error en la esquina: Unknown Error* | `CONVERTIDO_EN_TAREA` (`TASK-615`) |
| *OBS-002* | 2026-11-07 | Self Onboarding | Mejora | Baja | *Al ingresar la contraseña del usuario principal administrador, pedir confirmacion o en su defecto un segundo campo para que vuelva a escribirla y validar que tenga coincidencia* | `CONVERTIDO_EN_TAREA` (`TASK-621`) |
| *OBS-003* | 2026-11-07 | Empresas y sucursales | Cambio | Alta | *Al crear una nueva sucursal, no se inicializa correctamente, tampoco se actualiza el selector de sucursal, revisar el flujo de creacion de sucursal* | `CONVERTIDO_EN_TAREA` (`TASK-614`) |
| *OBS-004* | 2026-11-13 | Backoffice | UI/UX | Baja | *El backofficce frontend no cuenta con modo oscuro* | `CONVERTIDO_EN_TAREA` (`TASK-622`) |
| *OBS-005* | 2026-11-13 | Backoffice | Mejora | Media | *Auditoría y factorización de componentes visuales reutilizables en Frontends (React y Angular) + regla de diseño en el proceso SDLC* | `CONVERTIDO_EN_TAREA` (`TASK-620`) |
| *OBS-006* | 2026-11-13 | Backoffice (`/plans`) | Mejora / UX | Baja | *Clarificar/permitir creación de planes ilimitados gratis (campo vacío vs `0`) y añadir categorización/visibilidad de planes para ocultar planes especiales en Onboarding* | `CONVERTIDO_EN_TAREA` (`TASK-623`) |
| *OBS-007* | 2026-11-13 | Backoffice (`/platform/observability`) | Mejora / UI/UX | Media | *Agregar buscador en Loggers en Caliente, filtro por detalle + visor JSON formateado/dinámico + paginación en Auditoría Inmutable* | `CONVERTIDO_EN_TAREA` (`TASK-616`) |
| *OBS-008* | 2026-11-13 | Backoffice (`/platform/admins`) | Cambio / Mejora | Baja | *Hacer opcional el email al crear administradores de plataforma y permitir configurar/flexibilizar política de contraseñas desde el Backoffice* | `CONVERTIDO_EN_TAREA` (`TASK-624`) |
| *OBS-009* | 2026-11-13 | Backoffice (`/tenants`) | Cambio / Mejora | Media | *Hacer opcionales los campos dirección de sucursal y email del admin al aprovisionar un nuevo inquilino* | `CONVERTIDO_EN_TAREA` (`TASK-617`) |
| *OBS-010* | 2026-11-13 | Backoffice (`/tenants`) | Mejora / Cambio | Media | *Diferenciar estado inicial de inquilino aprovisionado que aún no ha iniciado sesión ni usado el sistema (ej. Pendiente de activación / Sin primer ingreso)* | `CONVERTIDO_EN_TAREA` (`TASK-618`) |

*(Actualiza esta tabla resumen al agregar nuevos ítems para tener visibilidad inmediata).*
*No es indispensable detallar abajo en caso de ser algo pequeño.*

---

## 📝 Guía de Tipos y Severidades

- **Tipo:**
  - `BUG`: Error de lógica, excepción, cálculo incorrecto o pantalla rota.
  - `UI/UX`: Detalle visual, padding/margin desalineado, tipografía, contraste o usabilidad.
  - `MEJORA`: Idea de valor, atajo de teclado, optimización de flujo existente.
  - `CAMBIO`: Modificación a una regla de negocio o comportamiento previamente acordado.
- **Severidad / Prioridad:**
  - `CRÍTICA`: Bloquea la operación diaria, pérdida o inconsistencia de datos/dinero.
  - `ALTA`: Fallo molesto o regla no cumplida sin workaround claro.
  - `MEDIA`: Detalle visible o inconsistencia menor que no detiene el flujo principal.
  - `BAJA`: Ajuste estético, sugerencia cosmética o preferencia personal.
- **Estados:**
  - `PENDIENTE`: Registrado por el usuario, esperando triage.
  - `EN_EVALUACION`: Analizando impacto o diseño técnico.
  - `CONVERTIDO_EN_TAREA`: Promovido formalmente a `docs/tasks.md` (ej. `TASK-6XX`).
  - `DESCARTADO`: No aplica o resuelto por diseño previo.
  - `RESUELTO`: Subsanado y validado en el sistema.

---

## 📋 Plantilla para Nuevas Observaciones

Copia y pega este bloque al final del documento cuando encuentres un nuevo punto:

```markdown
### [OBS-XXX] Título conciso del hallazgo o cambio solicitado
- **Fecha:** YYYY-MM-DD
- **Módulo / Ruta:** (ej. POS `/pos`, Finanzas `/finance`, Catálogo `/catalog/menus`)
- **Tipo:** `BUG` | `UI/UX` | `MEJORA` | `CAMBIO`
- **Severidad:** `CRÍTICA` | `ALTA` | `MEDIA` | `BAJA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual (Qué ocurre):**
  - Describir brevemente qué pasa actualmente o qué se ve en pantalla.
- **Comportamiento Esperado (Qué debería ocurrir):**
  - Explicar cómo debería comportarse según la lógica de negocio o diseño deseado.
- **Pasos para Reproducir (si aplica):**
  1. Paso 1
  2. Paso 2
  3. Resultado observado
- **Notas Adicionales / Captura / Contexto:**
  - Dispositivo/resolución (móvil/escritorio), usuario/rol usado, datos demo específicos, etc.
- **Acción Post-Triage (Completado por el Agente cuando se procese):**
  - *Tarea asignada:* (ej. `TASK-6XX` en `docs/tasks.md`)
  - *Fecha de conversión:*
```

---

## 🗂️ Registro Detallado de Observaciones

---

### [OBS-006] Planes ilimitados gratis y categorización/visibilidad en Onboarding
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** Backoffice `/plans` (y futuro flujo de Onboarding ERP)
- **Tipo:** `MEJORA` / `UI/UX`
- **Severidad:** `BAJA`
- **Estado:** `CONVERTIDO_EN_TAREA`
- **Comportamiento Actual:**
  - Al crear un plan ilimitado y gratis desde `/plans`, no queda claro en la UI si para "ilimitado" se debe dejar el campo vacío (`null`) y al ingresar `0` no permite crearlo.
  - No existe un categorizador ni bandera de visibilidad en los planes para distinguir planes públicos (mostrables en Onboarding junto al plan de prueba) de planes especiales/internos.
- **Comportamiento Esperado:**
  - Clarificar o mejorar la UX de límites ilimitados (ej. dejar vacío = ilimitado / toggle explícito "Ilimitado", y permitir precio `0` para planes gratuitos).
  - Añadir categoría/visibilidad a `subscription_plans` (ej. `PUBLIC` / `ONBOARDING` vs `SPECIAL` / `INTERNAL`) para que en el futuro el Onboarding muestre solo los planes habilitados para el cliente.
- **Acción Post-Triage:**
  - *Tarea asignada:* `TASK-623` en `docs/tasks.md`
  - *Fecha de conversión:* 2026-11-13

---

### [OBS-007] Buscador en Loggers y mejoras de usabilidad/paginación en Auditoría Inmutable
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** Backoffice `/platform/observability`
- **Tipo:** `MEJORA` / `UI/UX`
- **Severidad:** `MEDIA`
- **Estado:** `CONVERTIDO_EN_TAREA`
- **Comportamiento Actual:**
  - La pestaña de *Loggers en Caliente* no cuenta con buscador para filtrar paquetes/clases rápidamente.
  - En *Auditoría Inmutable* (`platform_audit_logs`), no hay buscador sobre el campo de detalle, el detalle se muestra truncado/sin formato JSON legible e interactivo, y no cuenta con paginación para evitar traer todos los registros cuando la tabla crezca.
- **Comportamiento Esperado:**
  - Agregar buscador reactivo en *Loggers en Caliente*.
  - En *Auditoría Inmutable*: añadir búsqueda por detalle (además de actor/acción), visor/modal expandible con JSON formateado y **paginación real** en backend y frontend.
- **Acción Post-Triage:**
  - *Tarea asignada:* `TASK-616` en `docs/tasks.md`
  - *Fecha de conversión:* 2026-11-13

---

### [OBS-008] Email opcional y política de contraseña configurable en Admins de Plataforma
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** Backoffice `/platform/admins`
- **Tipo:** `CAMBIO` / `MEJORA`
- **Severidad:** `BAJA`
- **Estado:** `CONVERTIDO_EN_TAREA`
- **Comportamiento Actual:**
  - El campo `email` es obligatorio al dar de alta un administrador de plataforma.
  - La validación de contraseña es fija y muy estricta, sin opción de configurarla desde el Backoffice.
- **Comportamiento Esperado:**
  - Permitir crear administradores con `email` opcional.
  - Exponer configuración en Backoffice para ajustar la política de contraseñas de la plataforma si se desean contraseñas más sencillas.
- **Acción Post-Triage:**
  - *Tarea asignada:* `TASK-624` en `docs/tasks.md`
  - *Fecha de conversión:* 2026-11-13

---

### [OBS-009] Dirección y email opcionales al aprovisionar nuevo inquilino en Backoffice
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** Backoffice `/tenants` (Modal *Aprovisionar Nuevo Inquilino*)
- **Tipo:** `CAMBIO` / `MEJORA`
- **Severidad:** `MEDIA`
- **Estado:** `CONVERTIDO_EN_TAREA`
- **Comportamiento Actual:**
  - El formulario de aprovisionamiento exige dirección de sucursal y correo electrónico del administrador de forma obligatoria.
- **Comportamiento Esperado:**
  - Hacer opcional la dirección de sucursal y el email del administrador inicial (en consistencia con el flujo de self-onboarding del ERP).
- **Acción Post-Triage:**
  - *Tarea asignada:* `TASK-617` en `docs/tasks.md`
  - *Fecha de conversión:* 2026-11-13

---

### [OBS-010] Estado inicial diferenciador para inquilinos recién aprovisionados sin primer ingreso
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** Backoffice `/tenants` & `/tenants/:id`
- **Tipo:** `MEJORA` / `CAMBIO`
- **Severidad:** `MEDIA`
- **Estado:** `CONVERTIDO_EN_TAREA`
- **Comportamiento Actual:**
  - Al crear un inquilino desde Backoffice, la vigencia dice "Sin plan" pero el estado muestra inmediatamente "Activo", aunque el cliente aún no haya ingresado ni usado el sistema.
- **Comportamiento Esperado:**
  - Contar con un estado o indicador claro (ej. `Pendiente de activación` / `Sin primer ingreso`) hasta que el inquilino inicie sesión u opere por primera vez.
- **Acción Post-Triage:**
  - *Tarea asignada:* `TASK-618` en `docs/tasks.md`
  - *Fecha de conversión:* 2026-11-13
