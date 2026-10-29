---
title: "Product Backlog: Prioridad P2 (Media Prioridad)"
category: "product-backlog"
priority: "P2"
tags:
  - backlog
  - p2
  - medium-priority
  - architecture
  - hardware
last_updated: "2026-10-28"
status: "verified"
---

# 🟡 Product Backlog — Prioridad P2 (Media Prioridad)

> [!NOTE]
> **Definición de Prioridad P2:** Mejoras arquitectónicas no bloqueantes, integraciones de hardware (impresión directa), estándares fiscales y optimizaciones de experiencia de usuario que agregan alto valor pero cuya ausencia no impide la operación básica.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P2

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-006** | Desacoplamiento y Compatibilidad Multi-Motor SQL | Arquitectura de Datos | En Backlog | Por Planificar |
| **PB-007** | Facturación Electrónica Oficial | Integraciones Fiscales | En Backlog | Por Planificar |
| **PB-008** | Módulo de Impresión Física ESC/POS (Comanderas de Red) | Hardware / POS | En Backlog | Por Planificar |
| **PB-013** | Evolución a Modelo de Dominio Rico en Entidades Transaccionales | Arquitectura de Dominio / DDD | En Backlog | Por Planificar |
| **PB-014** | Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex | Patrones de Diseño | En Backlog | Por Planificar |
| **PB-015** | Extracción de Generador de Secuencia Diaria de Comandas | Clean Code & Concurrencia | En Backlog | Por Planificar |
| **PB-036** | Catálogo Visual con Imágenes de Platos en POS y Toggle | Catálogo & POS / UI | Sprint 5 (Planificado) | `TASK-328` |
| **PB-040** | Tour Guiado Interactivo de Primera Experiencia (Product Tour) | Onboarding & UX | Completado (Sprint 3 - TASK-339) | `TASK-339` (Completado) |

---

## Detalle Exhaustivo de Requerimientos P2

### PB-006 [CORE] [ARQUITECTURA]: Desacoplamiento y Compatibilidad Multi-Motor SQL
- **Categoría:** Arquitectura de Datos
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Abstracción de dialectos relacionales en consultas JPA manteniendo estándar ANSI SQL para garantizar portabilidad futura.

---

### PB-007 [POS] [NEGOCIO]: Facturación Electrónica Oficial
- **Categoría:** Integraciones Fiscales
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Emisión de facturas electrónicas oficiales con firma digital, QR tributario y contingencia offline ante servicios de impuestos nacionales.

---

### PB-008 [POS] [INFRA]: Módulo de Impresión Física ESC/POS (Comanderas de Red)
- **Categoría:** Hardware / POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Envío directo de tickets vía TCP/IP a impresoras térmicas ESC/POS distribuidas por estaciones operativas (Cocina, Barra, Caja).

---

### PB-013 [CORE] [ARQUITECTURA]: Evolución a Modelo de Dominio Rico (Rich Domain Model) en Entidades Transaccionales
- **Categoría:** Arquitectura de Dominio / DDD
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Encapsulamiento de lógica transaccional dentro de `Order` y `CashShift`, evitando modelos anémicos y dispersión de reglas en servicios.

---

### PB-014 [INVENTORY] [ARQUITECTURA]: Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex
- **Categoría:** Patrones de Diseño / Módulo Inventory
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Inyección dinámica de estrategias mediante `Map<InventoryDeductionMode, DeductionStrategy>`, garantizando cumplimiento del Principio Abierto/Cerrado (OCP).

---

### PB-015 [POS] [ARQUITECTURA]: Extracción de Generador de Secuencia Diaria de Comandas (SRP en POS)
- **Categoría:** Clean Code & Concurrencia / Módulo POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Delegación de correlativos diarios (`T-1001`) a un servicio atómico especializado seguro ante alta concurrencia.

---

### PB-036 [CATALOG] [UI/UX]: Catálogo Visual con Imágenes de Platos en Punto de Venta y Toggle Parametrizable
- **Categoría:** Catálogo & POS / UI Operativa
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog (Sprint 5)`
- **Contexto:** En el punto de venta, la visualización de fotos de platos agiliza la identificación visual para el cajero. Sin embargo, en dispositivos de baja potencia o conexiones lentas, puede ser preferible una vista ligera de botones compactos.
- **Alcance Funcional y Técnico:**
  - Asociación de imagen al plato en catálogo mediante el motor `PB-035`.
  - Renderizado de tarjetas visuales en la cuadrícula de POS con miniaturas optimizadas WebP y carga diferida (*lazy loading*), con fallback a icono temático de categoría si no hay foto.
  - Parámetro de sucursal (`PB-026`): `POS_DISPLAY_DISH_IMAGES` (`true`: modo galería con fotos, `false`: modo compacto de texto y botones rápidos).
- **Criterios de Aceptación:**
  - El cajero puede activar o desactivar la visualización de imágenes desde la cabecera del POS según su preferencia o capacidad del dispositivo.
  - Las imágenes no ralentizan el scroll del catálogo ni consumen memoria excesiva en el navegador.

---

### PB-040 [UI/UX] [NEGOCIO]: Tour Guiado Interactivo de Primera Experiencia (Product Tour Onboarding)
- **Categoría:** Onboarding & UX / Primera Experiencia de Usuario
- **Prioridad:** `P2 - Media (Sprint 3)`
- **Estado:** `Completado (Sprint 3 - TASK-339)`
- **Contexto:** Al completar el registro asistido en el Asistente de Onboarding (`PB-001` / `TASK-333`), el dueño del restaurante es redirigido inmediatamente al Punto de Venta (`/pos`) o Dashboard con una sesión activa. Para maximizar la retención y comprensión del sistema, un tour guiado interactivo paso a paso debe acompañar sus primeras acciones sin resultar invasivo.
- **Alcance Funcional y Técnico:**
  - **Motor de Tour Ligero en Angular:** Integración de un servicio de tour guiado interactivo (ej. Driver.js encapsulado en servicio Angular Zoneless) que resalta elementos clave en pantalla y presenta tooltips flotantes con botones *"Siguiente"*, *"Atrás"* y *"Saltar tour"*.
  - **Ruta de Aprendizaje Rápida (4 Pasos Clave):**
    1. *Paso 1 (POS):* Abrir turno de caja inicial y seleccionar un plato de muestra.
    2. *Paso 2 (Cobro):* Simular el cobro de la primera comanda con método efectivo.
    3. *Paso 3 (Kardex):* Visualizar el descuento automático de ingredientes en tiempo real.
    4. *Paso 4 (Catálogo):* Enlace directo para cargar el menú definitivo mediante importación Excel o creación manual.
  - **Persistencia de Estado por Usuario:** Flag `onboarding_tour_completed` en `user_preferences` para no volver a mostrar el tour automáticamente una vez finalizado o descartado, manteniendo un botón de *"Repetir tour"* en la ayuda del sistema.
- **Criterios de Aceptación:**
  - El usuario puede descartar el tour en cualquier momento pulsando *"Saltar"* o la tecla `Escape`.
  - El tour se adapta perfectamente a pantallas móviles y de escritorio.
  - Completar o descartar el tour persiste el estado inmediatamente sin bloquear ninguna funcionalidad operativa.
