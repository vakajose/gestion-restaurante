# ADR-0013: Invariante Transaccional de Turno en POS, Desacoplamiento RBAC en Gastos y Políticas de Caja

- **Estado:** ACEPTADO
- **Fecha:** 2026-10-29
- **Autores:** Antigravity Architect & Product Owner
- **Módulos Afectados:** `modules.pos`, `modules.finance`, `core.security`, `modules.configuration`, `frontend`

---

## 1. Contexto y Problemática

Durante la retrospectiva del Sprint 3 (**OBS-POS-01** / **PB-041**), se identificaron fallas de integridad en la operativa de venta y caja:
1. **Fuga Financiera en POS:** El endpoint `POST /api/v1/pos/orders` procesa ventas y cobros sin verificar si existe un turno de caja abierto (`cash_shifts`) para la sucursal. Esto permite recaudar dinero fuera de turno, dejando los ingresos huérfanos del arqueo teórico y rompiendo la cuadratura contable.
2. **Falta de Trazabilidad de Cajero:** Las órdenes en backend almacenan la clave foránea `user_id` de forma relacional, pero no se expone `cashierId` en `OrderDto` ni en el ticket impreso.
3. **Acoplamiento Rígido de Roles en Gastos:** La autoaprobación de egresos de caja menor estaba acoplada a cadenas de texto quemadas (`ADMIN_TENANT`, `BRANCH_MANAGER`) en lugar de resolverse mediante permisos atómicos de la matriz RBAC (`PB-039`), impidiendo que roles personalizados (ej. *"Jefe de Cajeros"*) puedan autorizar gastos.
4. **Falta de Políticas de Apertura Parametrizables:** Clientes con modelos de supervisión estricta requieren que solo administradores o encargados abran la caja, impidiendo que el cajero inicie la jornada por su cuenta.
5. **Ruido en Terminales Móviles:** En el POS se mostraban badges permanentes de estado y un tag ambiguo de Dexie.js (`📥 1 pendiente(s)`), saturando el espacio vertical en smartphones y tablets.

---

## 2. Decisiones de Arquitectura

### 2.1 Invariante Estricto de Turno Abierto (Cero Excepciones)
- `OrderServiceImpl` inyecta obligatoriamente `FinancePublicApi`.
- Antes de procesar cualquier comanda en estado `PAID` o `IN_PREPARATION`:
  - Se consulta `financePublicApi.findActiveShift(tenantId, request.branchId())`.
  - Si no existe turno `OPEN`: Se aborta la transacción y se retorna **HTTP 409 Conflict** con formato RFC 7807:  
    `title: "Turno de Caja Requerido"`, `type: "https://api.restaurant.com/errors/shift-required"`, `detail: "No es posible registrar ventas: la caja de la sucursal se encuentra cerrada."`.
  - Si existe turno activo: Se asocia de manera mandatoria `order.setCashShiftId(activeShift.id())`. Jamás se persistirá una orden con `cash_shift_id = null`.

### 2.2 Política de Apertura de Turno Parametrizable
Se registra el parámetro en `modules.configuration`:
- **Clave:** `finance.shift_opening_policy`
- **Tipo:** `ENUM` (`ALL_AUTHORIZED`, `SUPERVISOR_ONLY`) | **Ámbito:** `BRANCH` (Sobreescribible por sucursal) | **Default:** `ALL_AUTHORIZED`.
- **Regla de Negocio:**
  - `ALL_AUTHORIZED`: Cualquier colaborador con el permiso RBAC `finance:shifts` puede ingresar fondo inicial y abrir la caja.
  - `SUPERVISOR_ONLY`: Solo usuarios que posean el permiso `finance:shifts_supervisor` (o rol administrador) pueden abrir el turno. Cajeros regulares quedan bloqueados con aviso: *"Caja Cerrada — Esperando apertura de turno por parte de un supervisor"*.

### 2.3 Gobernanza de Gastos Menores y Límites por Permisos
- Se erradican las comprobaciones de strings de roles (`role.equalsIgnoreCase(...)`). Todo egreso se rige por:
  - `finance:expenses_create`: Permite solicitar retiro de efectivo de la gaveta. **Siempre entra en `PENDING_APPROVAL`**.
  - `finance:expenses_approve`: Permite autorizar o rechazar solicitudes de egresos de otros colaboradores.
- **Techo Máximo de Gaveta (`finance.max_petty_cash_expense`):**
  - Cualquier egreso con `paidFromCashDrawer = true` que supere este valor (Default: $Bs.\ 100.00$) es rechazado por el backend para evitar descapitalizar la gaveta de caja chica.
- **Política de Autoaprobación (`finance.expense_allow_self_approval`):**
  - Parámetro `BOOLEAN` (Default: `false`).
  - Si es `false`: Un supervisor con `finance:expenses_approve` que solicite un gasto propio queda en `PENDING_APPROVAL` para revisión por un par o administrador (control dual).
  - Si es `true`: El gasto se aprueba inmediatamente al ser creado por un usuario con facultad de aprobación. El rol `ADMIN_TENANT` se autoaprueba siempre por jerarquía natural.

### 2.4 Trazabilidad Relacional Limpia
- Se aprovecha la clave foránea relacional ya existente en PostgreSQL: `orders.user_id REFERENCES app_users(id)`.
- `OrderDto` incorpora `UUID cashierId`.
- No se guarda el nombre de texto redundante en la base de datos; la capa de presentación y emisión de comprobantes térmicos/tickets resuelve el nombre del colaborador a partir de la sesión activa o servicio de usuarios.

### 2.5 Ergonomía Móvil y Visibilidad Silenciosa de Dexie.js
- **Estado Conectado & Sincronizado:** Pantalla 100% limpia sin badges de "En línea" estáticos que reduzcan el espacio vertical de venta en móviles (375px a 430px).
- **Estado Offline o Cola Pendiente:** Si `navigator.onLine == false` o `pendingCount > 0`, se despliega una barra superior de advertencia no intrusiva en color ámbar:  
  `🟠 Modo Offline (Sin conexión) · {count} órdenes locales guardadas en terminal`.

---

## 3. Consecuencias

### Positivas
- **Integridad Contable Absoluta:** Cero órdenes huérfanas fuera de turno; el 100% de los cobros en efectivo se concilian en el arqueo diario.
- **Flexibilidad Operativa:** El dueño del restaurante decide si confía la apertura de caja a los cajeros o si la reserva para supervisores.
- **Seguridad RBAC Limpia:** La plataforma soporta roles dinámicos a medida (*"Jefe de Barra"*, *"Encargado de Caja"*) sin alterar código Java.
- **UX Libre de Ruido en Smartphones:** Máximo aprovechamiento de pantalla para la comanda y catálogo.

### Negativas / Mitigaciones
- Clientes que operaban ventas sin abrir turno recibirán un error 409. **Mitigación:** El POS muestra un banner claro con botón directo (CTA) para *"Abrir Turno con Fondo Inicial"* en 1 clic.
