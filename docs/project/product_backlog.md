# Product Backlog y Registro de Nuevas Funcionalidades

Este documento centraliza el Product Backlog del sistema, ordenado estrictamente por **nivel de prioridad**. Aquí se registran las iniciativas, requerimientos y decisiones arquitectónicas que surgen durante el desarrollo pero que **no se implementarán de forma inmediata**, sirviendo de repositorio para la planificación de futuros sprints.

---

## Matriz de Priorización

| Nivel | Clasificación | Criterio de Entrada |
| :--- | :--- | :--- |
| **P0** | **Crítica / Máxima** | Bloqueante estructural para el modelo de negocio, gobernanza SaaS o ciclo de vida inicial del inquilino (previo a rollout). |
| **P1** | **Alta** | Ampliación clave de la operación del cliente (multi-sucursal, consolidación). |
| **P2** | **Media** | Integraciones periféricas (hardware, tributaria, soporte multi-motor). |
| **P3** | **Baja / Futuro** | Mejoras de experiencia operativa avanzada (KDS avanzado, plano de mesas). |

---

## 1. Prioridad P0 (Máxima Prioridad)

### PB-001: Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante)
- **Categoría:** Ciclo de Vida del Tenant / Onboarding
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto:** Actualmente el sistema asume la existencia estática o previa de tenants en base de datos. Se requiere un flujo guiado y automatizado para dar de alta a nuevos clientes comerciales.
- **Alcance Funcional:**
  - Registro inicial del cliente / empresa (Razón Social, Nombre Comercial, Identificación Fiscal/NIT, logo, sector).
  - Creación y configuración de la primera sucursal principal:
    - Nombre, dirección física, teléfono.
    - Zona horaria de la sucursal (según [ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_timezone_standard.md), por defecto `America/La_Paz`).
    - Moneda base y símbolos monetarios.
  - Asignación y creación de la cuenta del Administrador Propietario del Restaurante.
  - Parametrización inicial operativa (política de inventario predeterminada, turnos de caja iniciales).

---

### PB-002: Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner)
- **Categoría:** Gobernanza SaaS / Seguridad
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto:** Se requiere un rol administrativo exclusivo para los desarrolladores y dueños del sistema (proveedores del servicio), diferenciado de los administradores de los restaurantes inquilinos.
- **Alcance Funcional y Gobernanza:**
  - **Aislamiento Supra-Tenant:** Este rol opera fuera del ámbito de un `tenant_id` específico; tiene visibilidad y control sobre la totalidad de la plataforma.
  - **Gestión de Inquilinos (Tenants):** Activación, suspensión temporal, bloqueo por mora o baja de cuentas de clientes.
  - **Parámetros de Inicialización:** Configuración de semillas globales, catálogos base sugeridos y feature flags del sistema.
  - **Métricas y Monitoreo de Rendimiento:**
    - Tiempos de respuesta y rendimiento de autenticación/login.
    - Tasa de error general, latencia de base de datos y volumen de transacciones globales.
    - Detección de abusos, intentos fallidos de autenticación y auditoría de seguridad a nivel infraestructura.

---

### PB-003: Backoffice Independiente (Frontend Desacoplado + BFF Dedicado)
- **Categoría:** Arquitectura / Plataforma
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto y Justificación:**
  - Las herramientas del Árbitro del Sistema no deben coexistir dentro del frontend operativo de los clientes (restaurantes).
  - Incluir el backoffice en el frontend del cliente sobrecargaría el bundle innecesariamente, expondría código y rutas internas a usuarios finales (superficie de ataque) y acoplaría despliegues de soporte con la operación de los restaurantes.
- **Lineamientos Arquitectónicos Preliminares:**
  - **Frontend Independiente:** Una aplicación web propia (Backoffice Web SPA), con un diseño orientado a consola de administración, métricas y gestión de tenants.
  - **BFF Dedicado (Backend For Frontend):** Capa o API de backend exclusiva para el Backoffice, encargada de orquestar operaciones a nivel plataforma, parámetros de rendimiento y administración de tenants con controles de seguridad y rate limiting especializados.
  - *Nota:* La arquitectura final, límites modulares y estrategia de despliegue serán definidos y consensuados detalladamente con el usuario antes de iniciar su implementación.

---

## 2. Prioridad P1 (Alta Prioridad)

### PB-004: Transferencias de Insumos entre Sucursales (`StockTransfer`)
- **Categoría:** Inventario Multi-Sucursal
- **Estado:** `En Backlog`
- **Alcance:**
  - Solicitud, despacho y confirmación de recepción de insumos entre sucursales del mismo inquilino.
  - Registro de movimientos en Kardex en ambas sucursales con control de stock en tránsito.

### PB-005: Consolidación Financiera y Reportería Multi-Sucursal
- **Categoría:** Finanzas / BI
- **Estado:** `En Backlog`
- **Alcance:**
  - Vista ejecutiva global para el dueño del restaurante con múltiples sucursales.
  - Rentabilidad agregada, comparativa de ventas entre locales y análisis consolidado de mermas y gastos.

---

## 3. Prioridad P2 (Media Prioridad)

### PB-006: Desacoplamiento y Compatibilidad Multi-Motor SQL
- **Categoría:** Arquitectura de Datos
- **Estado:** `En Backlog`
- **Alcance:**
  - Complementar documentación y patrones de diseño (Repository, Strategy, abstracción de dialectos) para permitir la integración con otros motores relacionales sin requerir reescrituras profundas de código de negocio.

### PB-007: Facturación Electrónica Oficial
- **Categoría:** Integraciones Fiscales
- **Estado:** `En Backlog`
- **Alcance:**
  - Emisión de facturas electrónicas e integración con servicios tributarios nacionales normados.

### PB-008: Módulo de Impresión Física ESC/POS (Comanderas de Red)
- **Categoría:** Hardware / POS
- **Estado:** `En Backlog`
- **Alcance:**
  - Envío directo de tickets de comanda y recibos a impresoras térmicas ESC/POS conectadas a la red local (TCP/IP).

---

## 4. Prioridad P3 (Baja Prioridad / Futuro)

### PB-009: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Estado:** `En Backlog`
- **Alcance:**
  - Configuración y visualización gráfica del salón por zonas/mesas con indicación de estado en tiempo real (libre, ocupada, por cobrar).

### PB-010: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Estado:** `En Backlog`
- **Alcance:**
  - Pantalla táctil para cocineros con cronómetros por plato/comanda y métricas de tiempos de preparación.