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
| **PB-036** | Catálogo Visual con Imágenes de Platos en POS y Toggle | Catálogo & POS / UI | Completado (Sprint 5) | `TASK-506` |
| **PB-040** | Tour Guiado Interactivo de Primera Experiencia (Product Tour) | Onboarding & UX | Completado (Sprint 3 - TASK-339) | `TASK-339` (Completado) |
| **PB-045** | Perfil de Usuario Autoservicio y Nombres Reales | Auth / UI/UX | Completado (Sprint 5) | `TASK-509` |
| **PB-047** | Humanización y Etiquetas de Consola de Parámetros | Configuración / UX | Completado (Sprint 5) | `TASK-510` |
| **PB-048** | Asistente de Códigos Secuenciales y Emojis en Categorías | Catálogo / UI/UX | Completado (Sprint 5) | `TASK-507` |
| **PB-050** | Configuración Dinámica y Duplicación de Plantillas de Menú en Apertura de Turno | Catálogo & Turnos POS / UX | En Backlog (Retrospectiva S4) | Por Planificar |


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
- **Prioridad:** `P2 - Media`
- **Estado:** `Parcial (Base funcional en TASK-339; Rediseño de flujo de transición y cierre en Retrospectiva Sprint 3 - OBS-UI-10)`
- **Contexto:** Si bien se integró Driver.js en Sprint 3, se detectó que al pulsar el botón de acción para ir a la función recomendada, la guía no se cierra adecuadamente y bloquea la navegación. Se requiere rediseñar el flujo del tour para que las transiciones entre rutas y el cierre del popover sean fluidos y naturales.
- **Alcance Funcional y Técnico:**
  - Rediseño de hooks de navegación en el servicio de Driver.js: captura de clics en CTAs internos cerrando el paso antes de emitir la navegación Angular.
  - Ajuste del contenido de los pasos del tour para enfocarlo estrictamente en las 3 acciones esenciales del dueño del restaurante.
- **Criterios de Aceptación:**
  - Al pulsar una acción que navega a otra pantalla, el tour finaliza o transiciona limpiamente sin dejar popovers huérfanos.

---

### PB-045 [AUTH] [UI/UX]: Perfil de Usuario Autoservicio, Visualización de Nombres Reales y Limpieza de Privilegios Técnicos en UI
- **Categoría:** Autenticación & Seguridad / Perfil de Usuario
- **Prioridad:** `P2 - Media`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-USR-01..02)`
- **Contexto:** Actualmente en la barra superior e inferior se muestra el rol técnico (`admin_tenant`) y el username en lugar del nombre real de la persona (`fullName`) y el título amigable de su rol (ej. "Administrador General"). Además, en la pantalla de "Mi Perfil" no se pueden editar datos propios y se despliega una lista cruda de recursos/privilegios técnicos innecesaria para el usuario común. Por último, en `/users` (Personal), la columna "Colaborador" no destaca el nombre completo.
- **Alcance Funcional y Técnico:**
  - **Header / User Badge:**
    - Despliegue de `user.fullName` como texto principal y la descripción legible del rol (`role.displayName`).
  - **Vista "Mi Perfil" Autoservicio:**
    - Edición de datos propios: Nombre completo, teléfono, correo de contacto y cambio de contraseña propia.
    - Eliminación de la lista cruda de permisos técnicos; en su lugar, tarjeta ejecutiva con los roles asignados y sucursales habilitadas.
  - **Tabla de Gestión de Personal (`/users`):**
    - Columna "Colaborador" destacando el Nombre Completo (`fullName`) en negrita, acompañado de su avatar/iniciales y el username/correo como texto secundario tenue.
- **Criterios de Aceptación:**
  - El usuario puede modificar su nombre y teléfono desde su perfil.
  - La interfaz ya no expone nombres técnicos de endpoints o recursos internos a usuarios finales.

---

### PB-047 [CONFIG] [UI/UX]: Humanización Visual, Etiquetas Descriptivas y Rediseño de la Consola de Parametría Multinivel
- **Categoría:** Configuración & Parámetros / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-CFG-01)`
- **Contexto:** En la consola de parámetros (`/settings/parameters`), los parches/tags de ámbito son visualmente toscos, con fuentes desproporcionadas. Se le otorgó más protagonismo visual a la clave técnica (`security.session_inactivity_timeout_minutes`) que al nombre descriptivo en español. Además, las listas desplegables muestran valores en inglés sin explicación contextual.
- **Alcance Funcional y Técnico:**
  - Rediseño de la tarjeta de parámetro: título legible en lenguaje natural como encabezado principal (`text-base font-semibold`), clave técnica en formato secundario badge o pill pequeño.
  - Etiquetas descriptivas para opciones de listas desplegables (ej. en vez de `DISABLED` / `OPTIONAL` / `MANDATORY`, mostrar *"Deshabilitado"*, *"Opcional al abrir turno"*, *"Obligatorio"*).
  - Modal o tooltip de ayuda extendida para parámetros con explicaciones largas y casos de uso prácticos.
- **Criterios de Aceptación:**
  - Cualquier administrador sin conocimientos técnicos entiende el propósito de cada parámetro y el impacto de sus valores antes de guardar.

---

### PB-048 [CATALOG] [UI/UX]: Asistente de Codificación Secuencial de Platos y Selector de Iconos/Emojis para Categorías
- **Categoría:** Catálogo de Platos & Categorías / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-CAT-04)`
- **Contexto:** El alta de platos exige inventar un código SKU alfanumérico manualmente, provocando fricción y errores. Además, al crear categorías de platos se muestran campos de texto libres en lugar de un selector visual de emojis/iconos SVG, y el orden visual no se calcula automáticamente.
- **Alcance Funcional y Técnico:**
  - **Generación Asistida de Códigos SKU:**
    - Al seleccionar una categoría (ej. "Bebidas" `BEB`), el sistema sugiere automáticamente el siguiente correlativo libre (ej. `BEB-001`, `BEB-002`). El usuario puede modificarlo si dispone de una codificación propia.
  - **Selector Visual de Emojis / Iconos SVG:**
    - Desplegable interactivo con catálogo predefinido de iconos gastronómicos comunes (🍕, 🍔, 🥗, ☕, 🍺, 🍰, 🥩) para asignar a la categoría en 1 clic.
    - Autocálculo del número de orden visual (`display_order` = último orden + 1) evitando colisiones.
- **Criterios de Aceptación:**
  - Crear un plato no requiere inventar un código manualmente.
  - Las categorías muestran su icono/emoji de manera consistente en el POS y catálogo sin fallos de renderizado.

---

### PB-050 [CATALOG/POS] [UI/UX]: Configuración Dinámica, Edición y Duplicación de Plantillas de Menú en Apertura de Turno
- **Categoría:** Catálogo de Platos & Operación POS / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog (Identificado en Retrospectiva Sprint 4)`
- **Contexto:** Al abrir el turno de caja en el Punto de Venta, el usuario selecciona una plantilla de menú existente. Sin embargo, en la dinámica real de cocina a veces se requiere ajustar un plato específico, cambiar el cupo diario o quitar un ítem no disponible ese día sin tener que abandonar el flujo y navegar a la sección de configuración de catálogo.
- **Alcance Funcional y Técnico:**
  - **Inspección In-Situ del Menú:**
    - Al seleccionar una plantilla en el modal de apertura de caja, se despliega una vista previa detallada de las secciones y platos incluidos con sus cupos asignados.
  - **Edición Rápida antes de Iniciar:**
    - Permitir activar/desactivar platos o modificar cupos pactados directamente en el modal de apertura.
  - **Guardado y Duplicación:**
    - Opción 1: *Guardar e Iniciar Turno* (actualiza la asignación del turno actual).
    - Opción 2: *Guardar como Nueva Plantilla y Duplicar* (preserva la plantilla original y crea una variante específica, ej. "Menú Lunes - Versión Lluviosa").
- **Criterios de Aceptación:**
  - El cajero o administrador puede editar la selección de platos del turno sin salir del modal de apertura.
  - Si se elige duplicar, se persiste una nueva plantilla en `daily_menu_templates` y se asocia al turno activo de forma transparente.

