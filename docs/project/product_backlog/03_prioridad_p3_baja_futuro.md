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
last_updated: "2026-10-28"
status: "verified"
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
| **PB-053** | Desacoplamiento y Extracción del Motor Transversal de Autorizaciones | Arquitectura & Seguridad | En Backlog (Futuras Iteraciones) | Por Planificar |

---

## Detalle Exhaustivo de Requerimientos P3

### PB-009 [SALON] [UI/UX]: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Editor interactivo drag & drop para diagramar salones y terrazas, con visualización gráfica del estado de mesas en tiempo real.

---

### PB-010 [POS] [UI/UX]: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Pantalla táctil interactiva para cocineros con cronómetros por comanda y métricas de tiempos de despacho.

---

### PB-038 [SALON] [NEGOCIO]: Módulo de Gestión de Reservas, Capacidad de Salón y Asignación de Mesas y Sillas
- **Categoría:** Salón & Atención / Experiencia de Cliente
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Futuras Iteraciones)`
- **Contexto:** Gestión de reservas previas por parte de clientes para asegurar disponibilidad en fechas y horarios pico, con control de capacidad física de comensales.
- **Alcance Funcional y Técnico:**
  - Agenda de reservas por fecha, turno y sucursal.
  - Registro de datos del comensal: nombre, teléfono, cantidad de personas, requerimientos especiales.
  - Asignación de mesas numeradas y sillas específicas para grupos grandes.
  - Estados de reserva: `PENDIENTE`, `CONFIRMADA`, `CLIENTE_SENTADO`, `CANCELADA`, `NO_SHOW` (no asistió).
- **Criterios de Aceptación:**
  - La asignación de reservas no permite sobrepasar la capacidad física máxima declarada para el salón en la franja horaria seleccionada.

---

### PB-051 [ONBOARDING] [GIS]: Integración de Proveedor de Mapas para Selección de Ubicación en Onboarding
- **Categoría:** Onboarding & Empresa / Servicios Geográficos (GIS)
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Identificado en Retrospectiva Sprint 4)`
- **Contexto:** Durante el asistente de onboarding, el usuario ingresa manualmente la dirección en texto. Para enriquecer la ficha de la sucursal matriz y soportar futuros cálculos de delivery o geolocalización de pedidos, es deseable contar con un mapa interactivo (ej. Leaflet / OpenStreetMap o Google Maps).
- **Alcance Funcional y Técnico:**
  - Selector de coordenadas geográficas (`latitude`, `longitude`) mediante pin arrastrable en mapa embebido.
  - Geocodificación inversa para auto-completar calle, zona y ciudad al mover el pin.
- **Criterios de Aceptación:**
  - Si el usuario habilita el mapa o arrastra el pin, las coordenadas se persisten en la tabla `branches`.
  - En caso de falta de conectividad o falla del proveedor de mapas, el ingreso manual de texto sigue funcionando sin bloqueos.

---

### PB-052 [THEME] [UI/UX]: Auditoría y Estandarización de Paletas Visuales para Temas Dinámicos en TailwindAdmin
- **Categoría:** Frontend UI/UX / Accesibilidad y Estilo
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Identificado en Retrospectiva Sprint 4)`
- **Contexto:** El personalizador de temas (`theme-customizer`) ofrece múltiples paletas de colores. Sin embargo, algunas combinaciones presentan contrastes mejorables en modo oscuro o diferencias sutiles que pueden consolidarse en un set de temas corporativos de alto estándar gastronómico.
- **Alcance Funcional y Técnico:**
  - Auditoría WCAG AA de contraste de color para cada preset disponible (modo claro y modo oscuro).
  - Normalización de los nombres de temas con identidad gastronómica profesional (ej. Bistro Blue, Emerald Kitchen, Amber Bakery, Sunset Grill, Classic Slate).
- **Criterios de Aceptación:**
  - Todas las paletas activas cumplen con el ratio de contraste mínimo 4.5:1 para texto normal en ambos modos (dark/light).

---

### PB-053 [CORE] [ARQUITECTURA]: Desacoplamiento y Extracción del Motor Transversal de Autorizaciones (core.authorization / modules.authorization)
- **Categoría:** Arquitectura & Seguridad / Gobernanza Multi-Módulo
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Identificado en Co-Diseño Sprint 5)`
- **Contexto:** En el Sprint 5, el motor de autorizaciones se implementa de manera ágil y especializada dentro de `modules.finance` para gastos menores y cancelaciones de turno. A medida que el ERP crezca con módulos de Compras masivas, Mermas extraordinarias de almacén central o Recursos Humanos, estas operaciones requerirán autorizaciones sin depender del módulo de Finanzas.
- **Alcance Funcional y Técnico:**
  - Extraer la entidad `AuthorizationRequest` y el servicio de aprobación hacia un módulo o paquete transversal (`core.authorization` o `modules.authorization`).
  - Desacoplar la dependencia de `cash_shift_id` para permitir solicitudes originadas en compras, inventario o RRHH fuera de turnos de caja.
  - Exponer interfaz pública `AuthorizationPublicApi` para que cualquier módulo solicite o valide aprobaciones con firma criptográfica (PIN / JWT).
- **Criterios de Aceptación:**
  - Las autorizaciones de compras o inventario no tienen dependencias en el módulo de finanzas.
  - 100% de verificación modular con `ApplicationModules.verify()`.


