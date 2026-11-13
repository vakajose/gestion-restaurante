---
title: "Batería de Pruebas E2E y Matriz de Certificación — Sprint 4"
category: "testing"
tags:
  - testing
  - qa
  - e2e
  - sprint-4
  - acceptance
  - test-battery
last_updated: "2026-10-30"
status: "specification"
---

# 🧪 Batería de Pruebas de Aceptación y Certificación E2E — Sprint 4

> **Documento de Especificación de QA para TASK-412 (PB-046)**  
> **Alcance:** Cobertura exhaustiva de todos los casos de uso implementados en el Sprint 4: Blindaje Financiero de Turno, UI Kit TailwindAdmin, Menús Diarios y Recetas Flexibles, Reingeniería de Control de Caja (`/cash-control`) vs Finanzas (`/finance`), y Ergonomía Móvil.

---

## 1. Arquitectura de la Suite de Pruebas y Criterios de Aceptación

Esta batería define la especificación técnica y de negocio que guiará la construcción de la suite automatizada en **TASK-412** (`scripts/e2e/run_sprint4_e2e_battery.js` y `frontend/e2e/shift-lifecycle.spec.ts`).

### 1.1. Perfiles de Usuario y Roles de Prueba
- **Cajero Operativo (`cajero1`):** Rol `CASHIER`. Operación diaria exclusiva en `/pos` y `/cash-control`. Sin permisos de aprobación de gastos ni auditoría gerencial.
- **Administrador de Sucursal / Tenant (`admin`):** Rol `ADMIN_TENANT` / `BRANCH_MANAGER`. Acceso a `/finance`, aprobación de gastos, creación de plantillas de menú en `/catalog/menus` y auditoría de discrepancias.

### 1.2. Invariantes de Negocio Auditadas
1. **Invariante de Caja Abierta (AC-POS-01):** Imposible vender, cobrar o registrar órdenes sin una sesión de caja activa con estado `OPEN` (HTTP 409 `SHIFT_REQUIRED_ERROR`).
2. **Invariante de Integridad de Cupos (AC-MENU-01):** Al agotarse el cupo de un plato del Menú Diario, el sistema bloquea su adición al carrito e impide sobreventas atómicamente.
3. **Invariante de Deducción de Gaveta (AC-FIN-01):** Los gastos de caja menor marcados para deducción de efectivo solo descuentan del saldo teórico de gaveta cuando han sido formalmente aprobados por un supervisor/administrador.
4. **Invariante de Arqueo Exacto (AC-FIN-02):**
   $$E_{\text{esperado}} = F_{\text{inicial}} + V_{\text{efectivo}} - G_{\text{aprobados}}$$
   Cualquier diferencia entre el conteo físico y el teórico queda registrada como sobrante o faltante.

---

## 2. Catálogo de Casos de Prueba (Sprint 4)

| ID Caso | Módulo / Feature | Descripción del Escenario | Tipo | Resultado Esperado |
| :--- | :--- | :--- | :--- | :--- |
| **TC-POS-010** | Blindaje POS | Bloqueo estricto de catálogo y carrito sin turno de caja activo | E2E Bloqueo | Banner reactivo de caja cerrada, catálogo no seleccionable y botón CTA visible. |
| **TC-POS-011** | Blindaje POS | Apertura rápida de turno desde modal directo en POS con saldo inicial | E2E Transaccional | Turno creado en BD (`OPEN`), POS desbloqueado reactivamente sin recarga. |
| **TC-MENU-001** | Catálogo / Menús | Creación y edición de plantilla de Menú Diario en `/catalog/menus` | E2E CRUD | Plantilla persistida con tiempos (Entrada, Segundo, Postre, Bebida) y comboPrice. |
| **TC-MENU-002** | Menús / Apertura | Asignación de Menú Diario al turno con asignación de cupos pactados | E2E Transaccional | Registro en `shift_menu_assignments` con cupos iniciales editables por plato. |
| **TC-MENU-003** | POS / Menús | Visualización reactiva de Menú Diario en POS y decremento por venta | E2E Reactivo | Badge `Cupo: X` decrementa tras cada cobro; al llegar a 0 pasa a `AGOTADO` y se bloquea. |
| **TC-INV-005** | Inventario / BOM | Deducción polimórfica en Kardex según estrategia (BOM, unidad o libre) | E2E Integración | Movimientos de salida generados solo para insumos con BOM o unidades de stock. |
| **TC-FIN-008** | Control de Caja | Panel operativo `/cash-control`: KPIs limpios de gaveta y turnos activos | E2E UI | Desglose sin redundancias: Saldo Inicial, Ventas Efectivo, Gastos y Teórico. |
| **TC-FIN-009** | Control de Caja | Comandas en vivo cronológicas, filtros ("Mis Ventas", "Todas") y detalle | E2E Auditoría | Listado descendente en vivo, filtro por cajero operativo y modal de ticket completo. |
| **TC-FIN-010** | Finanzas / Gastos | Registro de gasto menor por cajero y aprobación por administrador | E2E Workflow | Estado `PENDING_APPROVAL` no descuenta; tras aprobación de admin descuenta de gaveta. |
| **TC-FIN-011** | Control de Caja | Cuadre de caja, arqueo ciego y cierre de turno | E2E Finanzas | Comparativa esperada vs real; cálculo de diferencia y cambio de estado a `CLOSED`. |
| **TC-FIN-012** | Finanzas / Auditoría | Historial de auditoría gerencial en `/finance` con turnos y discrepancias | E2E Auditoría | Turno cerrado registrado en tabla gerencial con detalle de auditoría. |
| **TC-UI-001** | UI / Ergonomía | Colapso universal de Sidebar a Mini-Rail (76px) en pantallas $\ge 768\text{px}$ | E2E Responsive | Botón hamburguesa alterna ancho fluido de 270px a 76px con tooltips nativos. |
| **TC-UI-002** | UI / Ergonomía | Supresión de sobrecarga vertical y subtítulos en móvil ($< 640\text{px}$) | E2E Mobile | Subtítulos estáticos ocultos (`hidden sm:block`) liberando $\ge 35\text{px}$ en POS y Finanzas. |
| **TC-UI-003** | UI / Ergonomía | Menú contextual táctil `[···]` en cabecera móvil de POS | E2E Mobile Touch | Despliegue de dropdown compacto con apertura de turno, control de caja y menús. |
| **TC-UI-004** | UI / Tipografía | Estandarización Monospace (`font-mono`) en stock de inventario y kardex | E2E UI Inspection | Números tabulares alineados a la derecha y tap targets $\ge 44\times 44\text{px}$. |

---

## 3. Especificación Detallada de los Flujos de Prueba (E2E Journeys)

```mermaid
sequenceDiagram
    autonumber
    actor C as Cajero1
    actor A as Admin
    participant POS as Punto de Venta (/pos)
    participant CC as Control de Caja (/cash-control)
    participant FIN as Finanzas (/finance)
    participant BD as Backend & PostgreSQL

    Note over C,POS: 1. Intento de Venta con Caja Cerrada (Invariante)
    C->>POS: Ingresa a /pos sin turno activo
    POS-->>C: Banner Caja Cerrada & Catálogo Bloqueado (TC-POS-010)

    Note over C,POS: 2. Apertura con Menú Diario y Cupos
    C->>POS: Clic en "Abrir Turno de Caja"
    C->>POS: Fondo inicial Bs. 150.00 + Menú "Almuerzo Ejecutivo" (3 cupos)
    POS->>BD: POST /api/v1/finance/shifts/open & assign-menu
    BD-->>POS: HTTP 201 Created (Shift OPEN)
    POS-->>C: Desbloqueo reactivo & Banner Menú Activo (TC-POS-011, TC-MENU-002)

    Note over C,POS: 3. Ventas de Menú y Agotamiento de Cupos
    loop 3 Ventas Consecutivas
        C->>POS: Selecciona Plato Menú Diario (Efectivo Bs. 25.00)
        POS->>BD: POST /api/v1/pos/orders/pay
        BD-->>POS: Cobro exitoso & Decremento atómico cupo (3->2->1->0)
    end
    POS-->>C: Badge cambia a "AGOTADO" y deshabilita adición (TC-MENU-003)

    Note over C,CC: 4. Auditoría de Comandas en Vivo
    C->>CC: Navega a /cash-control
    CC-->>C: KPIs: Inicial Bs. 150.00, Ventas Efectivo Bs. 75.00, Teórico Bs. 225.00
    CC-->>C: Lista 3 comandas en orden cronológico descendente (TC-FIN-009)
    C->>CC: Abre modal de detalle de comanda (T-XXXX)

    Note over C,A: 5. Flujo de Gasto Menor y Aprobación
    C->>CC: Registra solicitud de gasto "Verduras urgentes" Bs. 30.00 (Gaveta)
    CC->>BD: POST /api/v1/finance/expenses (PENDING_APPROVAL)
    CC-->>C: Efectivo Teórico se mantiene en Bs. 225.00 (sin deducir)
    A->>FIN: Ingresa como Admin a /finance -> Gastos de Caja Chica
    A->>FIN: Aprueba solicitud de gasto
    FIN->>BD: PUT /api/v1/finance/expenses/{id}/approve
    C->>CC: Refresca /cash-control: Efectivo Teórico descuenta a Bs. 195.00 (TC-FIN-010)

    Note over C,CC: 6. Cuadre de Gaveta y Cierre de Turno
    C->>CC: Clic en "Cerrar Turno y Realizar Arqueo"
    CC-->>C: Esperado Teórico = Bs. 195.00 (150 + 75 - 30)
    C->>CC: Ingresa Conteo Físico = Bs. 195.00 (Diferencia = Bs. 0.00)
    CC->>BD: POST /api/v1/finance/shifts/{id}/close
    BD-->>CC: Turno CLOSED
    CC-->>C: Pantalla reseteada; POS bloqueado nuevamente (TC-FIN-011)

    Note over A,FIN: 7. Auditoría Gerencial Posterior
    A->>FIN: Consulta pestaña Turnos Anteriores
    FIN-->>A: Turno registrado con Bs. 0.00 discrepancia y estado CERRADO (TC-FIN-012)
```

---

### Flujo E2E-1: Blindaje Invariante de Turno y Apertura Reactiva
- **Objetivo:** Verificar que el sistema no permite operar el POS sin turno abierto y que el modal de apertura reactiva el catálogo instantáneamente.
- **Precondiciones:**
  - Base de datos con turno cerrado para `cajero1`.
- **Pasos:**
  1. Iniciar sesión como `cajero1` y navegar a `/pos`.
  2. Verificar que el banner "Caja Cerrada" esté visible (`data-testid="shift-closed-banner"`).
  3. Intentar hacer clic en un producto del catálogo: el carrito permanece vacío ($0.00) y los botones están deshabilitados.
  4. Hacer clic en "Abrir Turno de Caja" (`data-testid="open-shift-cta-btn"`).
  5. En el modal, ingresar Saldo Inicial: `150.00`.
  6. Confirmar apertura.
- **Validaciones (Assertions):**
  - Toast de éxito: *"Turno de caja abierto exitosamente"*.
  - El banner desaparece y el catálogo se vuelve interactivo.
  - El indicador de estado muestra: *"Turno Abierto"*.

---

### Flujo E2E-2: Menú del Día, Cupos Reactivos y Agotamiento de Stock
- **Objetivo:** Validar el ciclo completo del menú del día: asignación al turno con cupo de 3 unidades, venta secuencial, decremento reactivo en vivo y bloqueo al agotarse.
- **Precondiciones:**
  - Plantilla de menú "Almuerzo Ejecutivo" configurada con plato "Milanesa de Res" (`comboPrice: 25.00`).
  - Turno abierto con cupo inicial de 3 unidades para la Milanesa.
- **Pasos:**
  1. En `/pos`, activar la pestaña / filtro de "Menús del Día".
  2. Observar el plato "Milanesa de Res" con el badge `Cupo: 3`.
  3. Agregar 1 unidad al carrito y cobrar en efectivo (`Bs. 25.00`). Confirmar venta.
  4. Observar que el badge del plato actualiza a `Cupo: 2`.
  5. Repetir la venta 2 veces más en efectivo.
- **Validaciones (Assertions):**
  - Tras la tercera venta, el badge muestra `AGOTADO` en color rojo.
  - La tarjeta del plato adquiere opacidad reducida (`opacity-60`) y el cursor es `not-allowed`.
  - Intentar hacer clic en el plato: no se agrega al carrito.
  - En la base de datos, `shift_menu_items.sold_quota` es 3 y `available_quota` es 0.

---

### Flujo E2E-3: Control de Caja en Vivo (`/cash-control`) y Visor de Comandas
- **Objetivo:** Verificar la nueva vista operativa de caja separada de finanzas, comprobando KPIs de gaveta y lista de órdenes cronológica en vivo.
- **Precondiciones:**
  - 3 ventas de menú cobradas en efectivo (`3 x Bs. 25.00 = Bs. 75.00`).
  - Saldo inicial de Bs. 150.00.
- **Pasos:**
  1. Navegar a `/cash-control` en el sidebar (Operaciones Diarias).
  2. Inspeccionar la barra de KPIs de gaveta:
     - Saldo Inicial: `Bs. 150.00`
     - (+) Ventas Efectivo: `Bs. 75.00`
     - (-) Gastos Caja Chica: `Bs. 0.00`
     - (=) Efectivo Teórico en Gaveta: `Bs. 225.00`
  3. En la sección inferior, verificar la pestaña "Comandas en Vivo":
     - Deben aparecer 3 tickets ordenados descendentemente (el más reciente primero).
  4. Probar los filtros rápidos:
     - Clic en "Mis Ventas": Muestra las 3 órdenes de `cajero1`.
     - Campo de búsqueda: Escribir el número del ticket más reciente (ej. `T-0003`) y verificar filtrado en tiempo real.
  5. Hacer clic en el botón de ver detalle (`data-testid="view-order-detail-btn"`):
     - Validar que el modal abre con los ítems correctos, subtotal Bs. 25.00, hora y método `CASH`.
- **Validaciones (Assertions):**
  - Cálculos matemáticos en KPIs 100% exactos sin redundancias.
  - Comandas ordenadas de forma cronológica descendente.
  - Modal de detalle renderiza sin errores de consola.

---

### Flujo E2E-4: Workflow de Gasto Menor de Gaveta y Control de Aprobación
- **Objetivo:** Verificar que un gasto solicitado por el cajero no descuenta de la gaveta hasta que es aprobado por un administrador en `/finance`.
- **Precondiciones:**
  - Turno activo con Efectivo Teórico de `Bs. 225.00`.
- **Pasos:**
  1. Como `cajero1`, en `/cash-control`, hacer clic en "Registrar Gasto".
  2. Ingresar: Categoría "Insumos Urgentes", Concepto "Cilantro y Verduras", Monto `30.00`, marcado con "Pagar desde Gaveta". Confirmar.
  3. Verificar que en la pestaña de gastos aparece como `PENDING_APPROVAL`.
  4. Inspeccionar el KPI de Efectivo Teórico: se mantiene en `Bs. 225.00` (los gastos pendientes no restan).
  5. Cambiar de sesión a `admin` y navegar a `/finance` -> Gastos de Caja Chica.
  6. En la tabla de aprobaciones, ubicar la solicitud de Bs. 30.00 y hacer clic en "Aprobar".
  7. Regresar a `/cash-control` como cajero y refrescar.
- **Validaciones (Assertions):**
  - El gasto cambia su estado a `APPROVED`.
  - El KPI de (-) Gastos de Caja Chica sube a `Bs. 30.00`.
  - El Efectivo Teórico en Gaveta disminuye automáticamente a `Bs. 195.00` ($225.00 - 30.00$).

---

### Flujo E2E-5: Arqueo Ciego, Cuadre de Gaveta y Cierre de Jornada
- **Objetivo:** Certificar el cierre formal del turno con verificación matemática y validación de diferencias.
- **Precondiciones:**
  - Efectivo Teórico esperado: `Bs. 195.00`.
- **Pasos:**
  1. En `/cash-control`, hacer clic en "Cerrar Turno y Realizar Arqueo".
  2. Verificar que el modal de arqueo despliega el desglose matemático:
     - Inicial (`150.00`) + Ventas Efectivo (`75.00`) - Gastos Gaveta (`30.00`) = Esperado (`195.00`).
  3. Ingresar en el campo de Efectivo Físico Contado: `195.00`.
  4. Verificar que el indicador de diferencia muestre: `Bs. 0.00 (Cuadre Exacto)`.
  5. Ingresar notas de cierre: *"Turno completado sin discrepancias"*.
  6. Confirmar cierre de turno.
- **Validaciones (Assertions):**
  - Toast de confirmación de cierre de caja.
  - El turno en base de datos cambia su estado a `CLOSED` con `closed_at` registrado.
  - La interfaz de `/cash-control` se resetea mostrando el estado de caja cerrada.
  - Al regresar a `/pos`, la venta queda nuevamente bloqueada con el banner de caja cerrada.

---

### Flujo E2E-6: Auditoría Gerencial en `/finance` y Kardex Polimórfico
- **Objetivo:** Asegurar que el administrador puede auditar el turno cerrado con sus métricas consolidadas y que el Kardex registró los movimientos adecuados.
- **Precondiciones:**
  - Turno anterior cerrado exitosamente.
- **Pasos:**
  1. Iniciar sesión como `admin` y navegar a `/finance` (Gestión de Sucursal).
  2. En la sección "Historial de Turnos y Arqueos Anteriores", ubicar el turno recién cerrado.
  3. Validar las columnas:
     - Cajero: `cajero1`
     - Inicial: `Bs. 150.00`
     - Ventas Efectivo: `Bs. 75.00`
     - Gastos Gaveta: `Bs. 30.00`
     - Esperado: `Bs. 195.00`
     - Contado: `Bs. 195.00`
     - Diferencia: `Bs. 0.00`
     - Estado: `CERRADO`
  4. Navegar a `/inventory` y verificar:
     - Movimientos de Kardex correspondientes a las deducciones de insumos del menú diario.
     - Tipografía estandarizada `font-mono text-right` en stocks y costos.
- **Validaciones (Assertions):**
  - Datos históricos consolidados sin pérdida de información transaccional.
  - El Kardex refleja salidas atómicas consistentes con las 3 ventas realizadas.

---

### Flujo E2E-7: Ergonomía Móvil y Navegación Adaptativa
- **Objetivo:** Certificar la adaptación fluida a smartphones ($< 640\text{px}$) y el modo Mini-Rail en escritorio ($\ge 768\text{px}$).
- **Precondiciones:**
  - Navegador en emulación móvil (viewport $390\times 844\text{px}$) y desktop ($1280\times 800\text{px}$).
- **Pasos y Validaciones:**
  1. **En Desktop ($\ge 1024\text{px}$):**
     - Hacer clic en el botón hamburguesa universal (`data-testid="sidebar-toggle-btn"`).
     - Verificar que el sidebar se contrae a un ancho de `76px` (`isMiniRailActive() = true`), ocultando textos y centrando íconos Solar.
     - Verificar que el área del POS gana ancho horizontal útil para el catálogo y la comanda.
     - Volver a presionar el botón: se expande a `270px`.
  2. **En Smartphone ($390\text{px}$):**
     - En `/pos`, verificar que el subtítulo técnico está oculto (`hidden sm:block`) y la cabecera es ultra-compacta ($\le 44\text{px}$).
     - Verificar la presencia del botón menú contextual `[···]` (`data-testid="pos-context-menu-btn"`).
     - Tocar `[···]`: Se despliega el dropdown con opciones (Abrir Turno, Control de Caja, Menú Diario, Sync Offline).
     - En `/cash-control` y `/finance`, verificar que la cuadrícula de KPIs se reorganiza a 2 columnas sin generar scroll horizontal.
     - Verificar que los botones de incremento/decremento en comanda cumplen el tamaño mínimo táctil de $44\times 44\text{px}$.

---

## 4. Matriz de Datos de Prueba para la Suite TASK-412

| Variable / Parámetro | Valor de Prueba | Propósito |
| :--- | :--- | :--- |
| **Usuario Cajero** | `cajero1` / `cajero123` | Validación de permisos acotados en caja |
| **Usuario Administrador** | `admin` / `admin123` | Aprobación de gastos y auditoría gerencial |
| **Sucursal Activa** | Sucursal Central (`demo-restaurant`) | Contexto multi-tenant aislado |
| **Saldo Inicial de Turno** | `Bs. 150.00` | Fondo base de gaveta para prueba |
| **Plantilla Menú Diario** | Almuerzo Ejecutivo (`Bs. 25.00`) | Validación de cupos y combo comboPrice |
| **Cupo Inicial del Plato** | `3 unidades` | Test de agotamiento reactivo exacto |
| **Gasto Menor de Gaveta** | `Bs. 30.00` ("Insumos Urgentes") | Test de retención de gaveta y aprobación |
| **Efectivo Físico Contado** | `Bs. 195.00` | Cuadre exacto con diferencia `Bs. 0.00` |
| **Viewport Móvil** | `390 x 844` (iPhone 14 / Pixel 7) | Ergonomía táctil y supresión de subtítulos |
| **Viewport Desktop** | `1280 x 800` | Mini-Rail de 76px y visualización estándar |

---

## 5. Próximo Paso de Ejecución (TASK-412)

Con esta especificación aprobada, la implementación de **TASK-412** procederá a:
1. Codificar el script automatizado determinista en `scripts/e2e/run_sprint4_e2e_battery.js` (o suite Playwright en `frontend/e2e/`).
2. Conectar contra el servidor local con servicios levantados (Spring Boot 8080 y Angular 4200).
3. Ejecutar de forma 100% determinista la secuencia continua: **Bloqueo sin turno $\rightarrow$ Apertura con Menú $\rightarrow$ 3 Ventas con cupo decreciente $\rightarrow$ Auditoría en vivo de comandas $\rightarrow$ Solicitud y Aprobación de Gasto $\rightarrow$ Arqueo matemático exacto $\rightarrow$ Verificación en Finanzas y Kardex $\rightarrow$ Certificación Móvil/Mini-Rail**.
4. Generar el reporte formal en `docs/testing/reporte_ejecucion_sprint4.md`.
