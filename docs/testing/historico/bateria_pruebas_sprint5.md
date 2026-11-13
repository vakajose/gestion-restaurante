---
title: "Batería de Pruebas E2E y Matriz de Certificación — Sprint 5"
category: "testing"
tags:
  - testing
  - qa
  - e2e
  - sprint-5
  - acceptance
  - test-battery
  - dod
last_updated: "2026-11-09"
status: "specification"
---

# 🧪 Batería de Pruebas de Aceptación y Certificación E2E — Sprint 5

> **Documento de Especificación de QA para TASK-511 (DoD Producción)**  
> **Alcance:** Cobertura exhaustiva de extremo a extremo de todas las capacidades funcionales del Sprint 5: Modificadores y Notas de Cocina (PB-020), Cancelaciones con PIN y Mermas (PB-031), Modalidades Takeout/Delivery y Tarifa de Envío (PB-032), Catálogo Visual e Imágenes (PB-036), SKU Asistido y Emojis de Categoría (PB-048), Histórico Gerencial de Turnos y Exportación CSV (PB-030), Perfil Autoservicio y Humanización de Personal (PB-045), y Consola de Parametría Humanizada con Catálogo JSON Desacoplado (PB-047).

---

## 1. Arquitectura de la Suite de Pruebas y Criterios de Aceptación

Esta batería define la especificación técnica y funcional que rige la suite automatizada de certificación de pre-producción para el cierre del Sprint 5 (`v0.5.0` / `v1.0.0-RC`).

### 1.1. Perfiles de Usuario y Roles de Prueba
- **Cajero Operativo (`cajero1` / `cajero123`):** Rol `CASHIER`. Operación cotidiana en `/pos` y `/cash-control`. Sin permisos de aprobación de gastos ni auditoría gerencial.
- **Supervisor de Turno / Encargado (`supervisor1` / `super123`):** Rol `BRANCH_MANAGER`. Posee PIN de autorización rápida configurado en su perfil autoservicio (`1234`).
- **Administrador de Empresa (`admin` / `admin123`):** Rol `ADMIN_TENANT` / `SUPERADMIN`. Acceso a `/settings/parameters`, `/finance`, `/catalog` y `/users`.

### 1.2. Invariantes de Negocio Auditadas (Sprint 5)
1. **Invariante de Modificadores y Kardex (AC-POS-05):**  
   Los modificadores con costo adicional incrementan de forma determinista el total del ítem en la comanda; si el modificador tiene insumo asociado, su salida en Kardex se descuenta atómicamente tras el cobro junto a la receta base.
2. **Invariante de Cancelación y PIN Forense (AC-POS-06):**  
   Una comanda comandada o cobrada no puede anularse sin un PIN válido de supervisor (`403 FORBIDDEN`). Al anularse con política `STRICT_WASTE`, se genera un movimiento de merma irreversible en Kardex; si la comanda se cobró en efectivo, se descuenta de la gaveta física como reintegro formal.
3. **Invariante de Modalidad de Despacho y Delivery Fee (AC-POS-07):**  
   Comandas `DELIVERY` aplican la tarifa configurada en `pos.delivery_fee_default` y muestran el badge destacado `DELIVERY` en el ticket; comandas `TAKEOUT` no exigen asignación de mesa y llevan el rótulo `PARA LLEVAR`.
4. **Invariante de Catálogo Visual Ergonomía (AC-CAT-01):**  
   El parámetro `pos.display_dish_images` alterna limpiamente entre botonera visual con imágenes centradas o cuadrícula compacta de alta velocidad sin romper el layout responsive.
5. **Invariante de Asistente SKU y Emojis (AC-CAT-02):**  
   Al crear categorías, se asigna un emoji visual y el orden correlativo se autocalcula (`display_order = max + 1`); los platos sugieren el SKU consecutivo (`PREF-001`) sin colisiones.
6. **Invariante de Auditoría Histórica y Exportación CSV (AC-FIN-03):**  
   `/finance` permite filtrar turnos por rango de fechas, cajero y discrepancia, inspeccionar el modal drill-down y exportar un CSV con BOM UTF-8 (`\uFEFF`) y delimitador `;` compatible directamente con Microsoft Excel.
7. **Invariante de Perfil y Humanización (AC-AUTH-02):**  
   Los usuarios actualizan su nombre real, teléfono y PIN en autoservicio; el header, sidebar y tablas de personal muestran nombres humanos en lugar de identificadores técnicos o strings crudos de permisos.
8. **Invariante de Parametría Humanizada y Catálogo JSON (AC-CFG-01):**  
   `/settings/parameters` renderiza títulos en lenguaje natural en español, opciones de negocio humanizadas desde el catálogo JSON desacoplado, popover contextual de ayuda `(?)` y buscador predictivo multicriterio.

---

## 2. Catálogo de Casos de Prueba (Sprint 5)

| ID Caso | Módulo / Requerimiento | Descripción del Escenario E2E | Tipo | Resultado Esperado |
| :--- | :--- | :--- | :--- | :--- |
| **TC-CFG-001** | Parámetros (PB-047) | Inspección de consola humanizada, tooltips contextuales y activación de flete | E2E Configuración | Sin enums crudos en inglés; popover `(?)` visible; flete de delivery activado. |
| **TC-CAT-001** | Catálogo / Emojis (PB-048) | Alta de categoría con selector visual de emoji (🍕) y orden correlativo | E2E CRUD | Categoría creada con icono 🍕 y correlativo autocalculado. |
| **TC-CAT-002** | Catálogo / SKU (PB-048) | Alta de plato con sugerencia de código correlativo (`PIZ-001`) y modificadores | E2E Catálogo | Código `PIZ-001` sugerido automáticamente; modificadores guardados. |
| **TC-AUTH-001**| Perfil / Autoservicio (PB-045) | Actualización de nombre real, teléfono y configuración de PIN de supervisor | E2E Seguridad | Nombre en negrita en sidebar; PIN almacenado con BCrypt; sin lista cruda de permisos. |
| **TC-POS-012** | POS / Modificadores (PB-020) | Venta `DINE_IN` con modificador con costo (Queso Extra +5 BOB) y notas | E2E Transaccional | Ticket suma subtotal correcto; nota impresa; Kardex descuenta receta y extra. |
| **TC-POS-013** | POS / Delivery Fee (PB-032) | Comanda `DELIVERY` con dirección de despacho y tarifa de flete | E2E Transaccional | Ticket con recargo de flete y rótulo `DELIVERY` destacado. |
| **TC-POS-014** | POS / Cancelación (PB-031) | Anulación de orden comandada solicitando PIN de supervisor | E2E Forense | Bloquea sin PIN; autoriza con PIN válido; registra merma estricta y reintegra gaveta. |
| **TC-FIN-013** | Caja / Arqueo (PB-030) | Registro de gasto menor y arqueo ciego al cerrar turno | E2E Finanzas | Efectivo teórico exacto considerando ventas, gastos y reintegros por anulación. |
| **TC-FIN-014** | Finanzas / Auditoría (PB-030) | Auditoría gerencial en `/finance`: filtros multicriterio y drill-down | E2E Auditoría | Desglose por medio de pago, detalle de comandas y egresos de caja chica. |
| **TC-FIN-015** | Finanzas / CSV (PB-030) | Exportación nativa CSV compatible con Microsoft Excel | E2E Exportación | Descarga directa de archivo `.csv` con prefijo BOM UTF-8 y delimitador `;`. |

---

## 3. Diagrama de Secuencia de la Jornada Completa (Master Business Journey)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador
    actor Supervisor as Supervisor
    actor Cajero as Cajero
    participant UI as Frontend Angular 22
    participant API as Backend Spring Modulith
    participant DB as PostgreSQL & Kardex

    Note over Admin,UI: 1. Parametrización Humanizada y Catálogo
    Admin->>UI: Navega a /settings/parameters
    UI-->>Admin: Consola con títulos claros, badges suaves y selector humanizado (TC-CFG-001)
    Admin->>UI: Activa Delivery Fee y establece tarifa 10.00 BOB
    Admin->>UI: Navega a /catalog -> Categoría con Emoji 🍕 y Plato con SKU Asistido PIZ-001 (TC-CAT-001, TC-CAT-002)

    Note over Supervisor,UI: 2. Perfil Autoservicio y Configuración de PIN
    Supervisor->>UI: Navega a /profile
    UI-->>Supervisor: Tarjeta ejecutiva sin lista cruda de permisos (TC-AUTH-001)
    Supervisor->>UI: Actualiza teléfono y configura PIN numérico "1234"
    UI->>API: PUT /api/v1/profile/pin (Hash BCrypt)
    API-->>UI: 200 OK (PIN configurado)

    Note over Cajero,UI: 3. Apertura de Turno y Ventas Especializadas
    Cajero->>UI: Abre turno con fondo inicial 100.00 BOB en /pos
    Cajero->>UI: Registra Comanda DINE_IN con Pizza + Queso Extra (+5 BOB) y Nota "Bien dorada" (TC-POS-012)
    UI->>API: POST /api/v1/pos/orders/pay (Efectivo 55.00 BOB)
    API->>DB: Registra venta y descuenta Kardex (masa + queso extra)
    
    Cajero->>UI: Registra Comanda DELIVERY con tarifa 10.00 BOB y cobro Efectivo 60.00 BOB (TC-POS-013)
    UI-->>Cajero: Comanda con badge DELIVERY destacado

    Note over Cajero,Supervisor: 4. Cancelación con Autorización PIN y Merma
    Cajero->>UI: Solicita anular orden comandada
    UI-->>Cajero: Modal solicita PIN de supervisor (TC-POS-014)
    Supervisor->>UI: Ingresa PIN "1234" y motivo "Cliente canceló por demora"
    UI->>API: POST /api/v1/pos/orders/{id}/cancel (STRICT_WASTE)
    API->>DB: Registra merma en Kardex y marca reembolso en gaveta

    Note over Cajero,UI: 5. Arqueo y Cierre de Turno
    Cajero->>UI: Navega a /cash-control y ejecuta Arqueo Ciego (TC-FIN-013)
    UI-->>Cajero: Teórico = Fondo + Ventas Efectivo - Reembolso Anulación
    Cajero->>UI: Confirma cierre con conteo físico exacto

    Note over Admin,UI: 6. Auditoría Gerencial y Exportación Excel
    Admin->>UI: Navega a /finance -> Histórico de Turnos (TC-FIN-014)
    Admin->>UI: Aplica filtro por fecha y cajero; abre modal Drill-Down
    Admin->>UI: Clic en "Exportar CSV" -> Descarga reporte compatible con Excel (TC-FIN-015)
```

---

## 4. Especificación Detallada de los Flujos de Prueba E2E

### Flujo E2E-1: Consola de Parámetros Humanizada y Activación de Flete
- **Objetivo:** Verificar la nueva presentación ejecutiva de parámetros sin nombres técnicos primarios y habilitar el servicio de envíos.
- **Pasos:**
  1. Iniciar sesión como `admin` y navegar a `/settings/parameters`.
  2. Verificar que cada tarjeta exhibe el nombre en español como título principal (`text-base font-semibold`) y la clave técnica en un badge secundario discreto.
  3. Probar el buscador predictivo escribiendo *"delivery"*: el sistema filtra reactivamente los parámetros de envíos.
  4. Hacer clic en el botón de ayuda `(?)` junto a "Habilitar Costo de Envío": validar que el popover despliega el impacto operativo y un ejemplo práctico de restaurante.
  5. Activar el toggle de `pos.delivery_fee_enabled` (cambia a *"Activado"* en verde).
  6. Configurar `pos.delivery_fee_default` con el monto `10.00` y guardar.
- **Validaciones:**
  - Valores actualizados y persistidos en backend mediante llamadas atómicas PATCH.
  - La interfaz responde de forma instantánea sin recargas de página.

---

### Flujo E2E-2: Catálogo Visual, Asistente SKU y Emojis de Categoría
- **Objetivo:** Certificar el alta ágil de categorías con iconos visuales y productos con codificación secuencial asistida.
- **Pasos:**
  1. En `/catalog`, abrir modal para crear nueva categoría.
  2. En el selector de emojis/iconos, seleccionar el emoji de pizza (🍕).
  3. Ingresar nombre: *"Pizzas Especiales"*. Observar que el prefijo sugerido es `PIZ` y el orden de despliegue se autocalcula. Guardar.
  4. Abrir modal para crear nuevo plato en la categoría *"Pizzas Especiales"*.
  5. Observar que el campo Código SKU sugiere automáticamente `PIZ-001`.
  6. Ingresar nombre: *"Pizza Pepperoni"*, precio base `50.00` BOB.
  7. En la pestaña Modificadores, agregar:
     - Grupo: *"Extras"*, Obligatorio: `false`, Múltiple: `true`.
     - Opción: *"Queso Extra"*, Precio Adicional: `5.00` BOB.
     - Opción: *"Masa Gruesa"*, Precio Adicional: `0.00` BOB.
  8. Guardar plato.
- **Validaciones:**
  - Categoría visible con su emoji en catálogo y POS.
  - Plato persistido con SKU `PIZ-001` y modificadores configurados.

---

### Flujo E2E-3: Perfil Autoservicio y Configuración de PIN de Supervisor
- **Objetivo:** Verificar la autonomía del usuario para mantener sus datos y clave de autorización rápida.
- **Pasos:**
  1. Iniciar sesión como `supervisor1` y navegar a `/profile`.
  2. Verificar la ausencia de tokens técnicos de privilegios (ej. `pos:orders:cancel`); se despliega una tarjeta ejecutiva con el rol *"Gerente de Sucursal"* y la sucursal asignada.
  3. En el formulario de contacto, actualizar:
     - Nombre Completo: *"Carlos Supervisor"*.
     - Teléfono: `+591 70012345`.
     Guardar cambios.
  4. En la sección de PIN de Autorización, ingresar nuevo PIN: `1234` y confirmar.
- **Validaciones:**
  - El sidebar y header actualizan inmediatamente el nombre visible a *"Carlos Supervisor"*.
  - En base de datos, `app_users.pin_hash` almacena el hash BCrypt del PIN.

---

### Flujo E2E-4: Comanda con Modificadores de Costo y Notas de Cocina
- **Objetivo:** Validar la venta en sala con personalización y cálculo exacto de subtotales.
- **Pasos:**
  1. Iniciar sesión como `cajero1`, abrir turno con saldo inicial `100.00` BOB en `/pos`.
  2. Seleccionar modalidad `DINE_IN` y Mesa 1.
  3. Agregar *"Pizza Pepperoni"* al carrito.
  4. En el modal/drawer de personalización de plato:
     - Seleccionar *"Queso Extra"* (+5.00 BOB).
     - Escribir nota de cocina: *"Bien dorada, poco orégano"*.
     - Confirmar adición.
  5. Verificar comanda: subtotal debe ser `55.00` BOB ($50.00 + 5.00$).
  6. Cobrar en efectivo con billete de `100.00` BOB (Cambio: `45.00` BOB).
- **Validaciones:**
  - Ticket emitido con desglose del modificador y nota de cocina visible.
  - El Kardex registra la salida de inventario de los ingredientes de la pizza más el queso extra.

---

### Flujo E2E-5: Comanda Delivery con Tarifa de Envío y Dirección
- **Objetivo:** Validar la modalidad de despacho a domicilio con aplicación automática de flete.
- **Pasos:**
  1. En `/pos`, cambiar modalidad a `DELIVERY`.
  2. Observar que el selector de mesas se oculta y se solicita información de despacho.
  3. Ingresar: Cliente *"Ana Gómez"*, Teléfono *"71234567"*, Dirección *"Av. América #450"*.
  4. Agregar plato base de `50.00` BOB.
  5. Verificar que el total de la orden incluye automáticamente la tarifa de flete configurada:
     $$\text{Total} = 50.00 + 10.00 = 60.00\text{ BOB}$$
  6. Cobrar en efectivo con monto exacto de `60.00` BOB.
- **Validaciones:**
  - El ticket y el visor de comandas muestran el badge destacado `DELIVERY`.
  - La tarifa de envío queda registrada como línea de concepto en la orden y en el totalizador de caja.

---

### Flujo E2E-6: Cancelación con PIN de Supervisor, Merma y Reintegro
- **Objetivo:** Certificar el protocolo de anulación segura post-comanda.
- **Pasos:**
  1. En `/pos` o `/cash-control`, ubicar una orden de `55.00` BOB cobrada en efectivo y hacer clic en *"Anular Comanda"*.
  2. El sistema despliega el modal de autorización con PIN.
  3. Intentar ingresar PIN incorrecto `0000`: el sistema rechaza la operación con mensaje de error forense.
  4. Ingresar PIN válido de supervisor: `1234`.
  5. Seleccionar motivo: *"Error de mesa"* y confirmar tratamiento de merma (`STRICT_WASTE`).
- **Validaciones:**
  - La orden cambia a estado `CANCELLED`.
  - Kardex registra movimiento de salida por merma (`WASTE`).
  - En la gaveta física de caja menor, se genera un registro de reintegro en efectivo de `55.00` BOB.

---

### Flujo E2E-7: Cuadre de Gaveta y Arqueo Ciego
- **Objetivo:** Validar el cuadre matemático del turno considerando ventas netas y devoluciones.
- **Pasos:**
  1. En `/cash-control`, verificar el cálculo en tiempo real de los KPIs de gaveta:
     $$\text{Esperado} = \text{Inicial (100.00)} + \text{Venta 1 (55.00)} + \text{Venta 2 (60.00)} - \text{Reintegro Anulación (55.00)} = 160.00\text{ BOB}$$
  2. Clic en *"Cerrar Turno y Realizar Arqueo"*.
  3. Ingresar Efectivo Físico Contado: `160.00` BOB.
  4. Verificar que el indicador muestra `Diferencia: 0.00 BOB (Cuadre Exacto)`.
  5. Confirmar cierre de turno.
- **Validaciones:**
  - Turno pasa a estado `CLOSED`.
  - El POS queda bloqueado para nuevas ventas hasta la próxima apertura.

---

### Flujo E2E-8: Auditoría Gerencial Multicriterio y Exportación CSV
- **Objetivo:** Certificar la inspección histórica en profundidad y la exportación de reportes limpios.
- **Pasos:**
  1. Iniciar sesión como `admin` y navegar a `/finance` -> Histórico de Turnos.
  2. Aplicar filtro de fecha *"Hoy"* y verificar la aparición del turno recién cerrado con `160.00` BOB.
  3. Hacer clic en el botón de ver detalle / auditoría (`drill-down`):
     - Validar que el modal despliega las 3 pestañas: Resumen de Medios de Pago, Comandas Asociadas y Egresos/Devoluciones.
  4. En la cabecera, hacer clic en *"Exportar Lista (CSV)"*.
  5. Abrir el archivo generado con un editor o visor:
     - Verificar presencia del prefijo BOM UTF-8 (`\uFEFF`).
     - Verificar formato con separador punto y coma (`;`).
- **Validaciones:**
  - El archivo se genera en el navegador sin invocar librerías pesadas en servidor.
  - Caracteres en español (acentos, 'ñ') se renderizan sin corrupción de codificación.

---

## 5. Matriz de Datos de Prueba para la Batería Sprint 5

| Variable / Campo | Valor de Prueba | Propósito |
| :--- | :--- | :--- |
| **Usuario Administrador** | `admin` / `admin123` | Configuración de parámetros y auditoría gerencial |
| **Usuario Supervisor** | `supervisor1` / `super123` | Autoservicio y PIN de autorización rápida (`1234`) |
| **Usuario Cajero** | `cajero1` / `cajero123` | Operación en mostrador POS y arqueo de caja |
| **Sucursal Activa** | Sucursal Central (`demo-restaurant`) | Contexto multi-tenant aislado |
| **Fondo Inicial de Caja** | `100.00 BOB` | Base para cálculo de gaveta |
| **Tarifa de Envío por Defecto** | `10.00 BOB` | Delivery fee parametrizado |
| **Plato Base de Prueba** | Pizza Pepperoni (`PIZ-001`, `50.00 BOB`) | Plato con SKU asistido y modificadores |
| **Modificador con Costo** | Queso Extra (`+5.00 BOB`) | Adicional con impacto en precio y Kardex |
| **Modificador sin Costo** | Masa Gruesa (`+0.00 BOB`) | Personalización sin impacto en subtotal |
| **PIN de Autorización** | `1234` | Clave numérica de 4 dígitos |
| **Efectivo Contado al Cierre**| `160.00 BOB` | Conteo físico para cuadre exacto ($0.00$ diferencia) |

---

## 6. Procedimiento de Ejecución y Certificación DoD

Para certificar formalmente el Sprint 5 y declarar el **Definition of Done (DoD Producción)**:
1. **Ejecución de Pruebas Unitarias e Integración:**
   - Backend: `./mvnw test` (100% pruebas pasando sin fallos).
   - Frontend: `npm run lint && npm test` (0 advertencias de linting, 100% pasando en Vitest).
2. **Ejecución de la Suite E2E de Pre-Producción:**
   - Base de datos aprovisionada limpiamente mediante `scripts/db/reset_database.sh`.
   - Ejecución de la suite determinista en Playwright o script runner especializado.
3. **Generación del Reporte Oficial:**
   - Consolidar resultados, capturas y métricas en `docs/testing/reporte_ejecucion_sprint5.md`.
