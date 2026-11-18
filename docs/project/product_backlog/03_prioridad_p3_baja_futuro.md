---
title: "Product Backlog: Prioridad P3 (Baja Prioridad / Futuro)"
category: "product-backlog"
priority: "P3"
tags:
  - backlog
  - p3
  - low-priority
  - future
  - salon
  - kds
last_updated: "2026-11-17"
status: "active"
---

# 🟢 Product Backlog — Prioridad P3 (Baja Prioridad / Futuro)

> [!NOTE]
> **Definición de Prioridad P3:** Funcionalidades avanzadas, módulos complementarios y extensiones futuras que se abordarán en etapas maduras del producto o ante demanda específica de mercado.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P2](./02_prioridad_p2_media.md)

---

## Índice de Ítems P3

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-009** | Mapa Visual Interactivo de Distribución de Mesas | UX Salón / POS | En Backlog | Por Planificar |
| **PB-010** | Panel KDS Táctil con Métricas de Despacho en Cocina | Operaciones / Cocina | En Backlog | Por Planificar |
| **PB-038** | Módulo de Gestión de Reservas, Capacidad de Salón y Asignación | Salón & Atención | En Backlog | Por Planificar |
| **PB-051** | Integración de Proveedor de Mapas para Selección de Ubicación en Onboarding | Onboarding & GIS | En Backlog (Retrospectiva S4) | Por Planificar |
| **PB-052** | Auditoría y Estandarización de Paletas Visuales para Temas Dinámicos | Frontend UI/UX | En Backlog (Retrospectiva S4) | Por Planificar |
| **PB-053** | Desacoplamiento y Extracción del Motor Transversal de Autorizaciones | Arquitectura & Seguridad | En Backlog (Co-Diseño S5) | Por Planificar |
| **PB-054** | Módulo Avanzado de Encargos Programados (`Scheduled Pickups & Kitchen Queue`) | Operaciones / POS | En Backlog (Co-Diseño S5) | Por Planificar |
| **PB-055** | Parametrización de Métodos de Pago Habilitados por Sucursal | Configuración & Finanzas | En Backlog (Co-Diseño S5) | Por Planificar |
| **PB-056** | Gobernanza de Eliminación Lógica (`Soft Deletes`) y Trazabilidad Histórica | Arquitectura & Datos | En Backlog (Co-Diseño S5) | Por Planificar |

---

## Detalle Exhaustivo de Requerimientos P3

<a id="pb-009"></a>
### PB-009 [SALON] [UI/UX]: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Editor interactivo drag & drop para diagramar salones y terrazas, con visualización gráfica del estado de mesas en tiempo real.

---

<a id="pb-010"></a>
### PB-010 [POS] [UI/UX]: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Pantalla táctil interactiva para cocineros con cronómetros por comanda y métricas de tiempos de despacho.

---

<a id="pb-038"></a>
### PB-038 [SALON] [NEGOCIO]: Módulo de Gestión de Reservas, Capacidad de Salón y Asignación de Mesas y Sillas
- **Categoría:** Salón & Atención / Experiencia de Cliente
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Futuras Iteraciones)`
- **Contexto:** Gestión de reservas previas por parte de clientes para asegurar disponibilidad en fechas y horarios pico, con control de capacidad física de comensales.
- **Alcance Funcional y Técnico:**
  - Agenda de reservas por fecha, turno y sucursal.
  - Registro de datos del comensal: nombre, teléfono, cantidad de personas, requerimientos especiales.
  - Asignación de mesas numeradas y sillas específicas para grupos grandes.
  - Estados de reserva: `PENDIENTE`, `CONFIRMADA`, `CLIENTE_SENTADO`, `CANCELADA`, `NO_SHOW`.
- **Criterios de Aceptación:**
  - La asignación de reservas no permite sobrepasar la capacidad física máxima declarada para el salón en la franja horaria seleccionada.

---

<a id="pb-051"></a>
### PB-051 [ONBOARDING] [GIS]: Integración de Proveedor de Mapas para Selección de Ubicación en Onboarding
- **Categoría:** Onboarding & Empresa / Servicios Geográficos (GIS)
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Retrospectiva Sprint 4)`
- **Contexto:** Durante el asistente de onboarding, el usuario ingresa manualmente la dirección en texto. Para enriquecer la ficha de la sucursal matriz y soportar futuros cálculos de delivery o geolocalización de pedidos, es deseable contar con un mapa interactivo (ej. Leaflet / OpenStreetMap).
- **Alcance Funcional y Técnico:**
  - Selector de coordenadas geográficas (`latitude`, `longitude`) mediante pin arrastrable en mapa embebido.
  - Geocodificación inversa para auto-completar calle, zona y ciudad al mover el pin.
- **Criterios de Aceptación:**
  - Si el usuario habilita el mapa o arrastra el pin, las coordenadas se persisten en `branches`; si no hay conectividad, el ingreso manual funciona sin bloqueos.

---

<a id="pb-052"></a>
### PB-052 [THEME] [UI/UX]: Auditoría y Estandarización de Paletas Visuales para Temas Dinámicos en TailwindAdmin
- **Categoría:** Frontend UI/UX / Accesibilidad y Estilo
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Retrospectiva Sprint 4)`
- **Contexto:** Auditoría WCAG AA de contraste para los presets de color disponibles en modo claro y oscuro, normalizando paletas con identidad gastronómica profesional.
- **Criterios de Aceptación:**
  - Todas las paletas activas cumplen el ratio mínimo de contraste `4.5:1` en ambos modos.

---

<a id="pb-053"></a>
### PB-053 [CORE] [ARQUITECTURA]: Desacoplamiento y Extracción del Motor Transversal de Autorizaciones (`core.authorization` / `modules.authorization`)
- **Categoría:** Arquitectura & Seguridad / Gobernanza Multi-Módulo
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Co-Diseño Sprint 5)`
- **Contexto:** Extraer `AuthorizationRequest` desde `modules.finance` hacia un módulo transversal para soportar autorizaciones en compras masivas, mermas extraordinarias o RRHH sin depender de `cash_shift_id`.
- **Criterios de Aceptación:**
  - Verificación modular limpia con `ApplicationModules.verify()`.

---

<a id="pb-054"></a>
### PB-054 [POS] [OPERACIONES]: Módulo Avanzado de Encargos Programados (`Scheduled Pickups & Kitchen Queue`)
- **Categoría:** Operaciones / POS & Cocina
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Co-Diseño Sprint 5)`
- **Contexto:** Evolucionar la modalidad `SCHEDULED_PICKUP` hacia agenda cronológica de entregas (`pickup_scheduled_time` en `TIMESTAMPTZ`), alertas previas a cocina y gestión de señas parciales.

---

<a id="pb-055"></a>
### PB-055 [CONFIG/FINANCE] [NEGOCIO]: Parametrización de Métodos de Pago Habilitados por Sucursal
- **Categoría:** Configuración & Finanzas / Medios de Pago
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Co-Diseño Sprint 5)`
- **Contexto:** Parámetros multinivel (`payment.cash_enabled`, `payment.digital_methods_enabled`) para restringir dinámicamente los medios de cobro disponibles por sucursal en el checkout del POS.

---

<a id="pb-056"></a>
### PB-056 [CORE] [ARQUITECTURA/DATOS]: Gobernanza de Eliminación Lógica (`Soft Deletes`) y Trazabilidad Histórica Inmutable
- **Categoría:** Arquitectura & Gobernanza de Datos / Integridad Transaccional
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Co-Diseño Sprint 5)`
- **Contexto:** Estandarizar columnas `deleted_at` / `deleted_by` y filtros JPA `@SQLRestriction("deleted_at IS NULL")` en entidades maestras para preservar integridad referencial histórica.
