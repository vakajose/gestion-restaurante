---
title: "Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Inbox de Triage)"
category: "feedback"
status: "living-document"
last_updated: "2026-11-07"
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
| *OBS-001* | 2026-11-07 | Parametros del sistema | Bug | Media | *Al ingresar a parametros del sistema y al navegar entre la opcion de empresa y sucursales salta un badge de error en la esquina: Unknown Error* | `PENDIENTE` |
| *OBS-002* | 2026-11-07 | Self Onboarding | Mejora | Baja | *Al ingresar la contraseña del usuario principal administrador, pedir confirmacion o en su defecto un segundo campo para que vuelva a escribirla y validar que tenga coincidencia* | `PENDIENTE` |
| *OBS-003* | 2026-11-07 | Empresas y sucursales | Cambio | Alta | *Al crear una nueva sucursal, no se inicializa correctamente, tampoco se actualiza el selector de sucursal, revisar el flujo de creacion de sucursal* | `PENDIENTE` |

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
  - `CONVERTIDO_EN_TAREA`: Promovido formalmente a `docs/tasks.md` (ej. `TASK-5XX`).
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
  - *Tarea asignada:* (ej. `TASK-5XX` en `docs/tasks.md`)
  - *Fecha de conversión:*
```

---

## 🗂️ Registro Detallado de Observaciones

*(A continuación se registran los hallazgos. Puedes editar, mover o eliminar según sea necesario).*

---

### [OBS-001] Ejemplo: Bloqueo de modal tras comanda rápida
- **Fecha:** 2026-11-07
- **Módulo / Ruta:** `/pos` (Punto de Venta)
- **Tipo:** `UI/UX`
- **Severidad:** `MEDIA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual:**
  - Al presionar enter muy rápido al cobrar, el toast de éxito se superpone momentáneamente sobre el modal de selección de turnos.
- **Comportamiento Esperado:**
  - El modal de cobro debe cerrar inmediatamente al emitirse la orden y el toast permanecer en `z-[9999]` sin bloquear botones de fondo.
- **Pasos para Reproducir:**
  1. Abrir `/pos` con turno activo.
  2. Agregar 1 producto al carrito.
  3. Cobrar en efectivo pulsando Enter repetidamente.
- **Notas Adicionales:**
  - Ocurre solo en navegadores con vista desktop 1920x1080.
- **Acción Post-Triage:**
  - *Tarea asignada:* Pendiente de triage.
  - *Fecha de conversión:* —
