# ADR-0019: Flujo de Cancelación de Pedidos, Motor de Autorizaciones de Turno, PIN Rápido y Tratamiento de Mermas (PB-031)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-07
- **Autores:** Squad Multi-Agente & Líder de Proyecto (Co-Diseño Interactivo)
- **Alcance:** POS, Finanzas (Turnos y Arqueos), Inventario (Kardex y Mermas), Seguridad (RBAC y PIN)
- **Requerimiento:** `PB-031` (Cancelación de Pedidos, Devolución y Kardex)
- **Tareas Relacionadas:** `TASK-503` [POS] [DISEÑO], `TASK-504` [POS] [NEGOCIO]

---

## 1. Contexto y Problemática

En la operación gastronómica real se suscitan desistimientos de clientes tras haber pagado la comanda, errores de digitación del cajero o fallas en cocina (platos quemados, insumos agotados no notificados a tiempo). La anulación de una venta cerrada plantea retos críticos de cuadre:
1. **Integridad de Dinero en Gaveta:** Si se cancela una orden cobrada en efectivo y se devuelve el importe al comensal, el dinero físico sale del cajón. Si el sistema no registra el egreso exacto (`REFUND`), el arqueo físico al final del turno arrojará un faltante injustificado.
2. **Integridad de Almacén y Kardex:** Al cobrarse la orden, los insumos de las recetas (BOM) ya fueron descontados. Si el plato **no se cocinó**, los ingredientes deben reincorporarse al stock disponible; pero si el plato **ya se preparó**, los insumos físicos salieron de la despensa y no pueden reingresarse como materia prima.
3. **Agilidad en Punto de Venta vs. Control Antifraude:** La anulación de una orden o la devolución de dinero es una operación altamente sensible que no puede quedar al arbitrio del cajero. No obstante, obligar al supervisor a abandonar su puesto para iniciar sesión en otra computadora cada vez que hay una cancelación o gasto menor paraliza la fila de cobro.
4. **Reutilización Opcional de Platos Preparados (Buffer de Vitrina):** En negocios de comida rápida o platos de alta rotación (ej. pizzas completas, hamburguesas, pollos, porciones de menú del día), un plato recién cocinado cuya comanda fue cancelada puede servirse inmediatamente al siguiente cliente sin tirarlo a la basura, cobrándolo sin volver a descontar insumos por segunda vez.

---

## 2. Decisiones Arquitectónicas

### A. Alcance de la Cancelación: Cancelación Atómica de Orden Completa
- Se adopta la **cancelación atómica de la orden completa** (`CANCELLED`).
- Si un cliente desiste de un ítem particular de una orden ya cerrada y pagada, se cancela la orden original completa, se procesa la devolución correspondiente y se emite de inmediato una nueva orden con los ítems que el cliente sí consumirá. Esto garantiza determinismo contable, tickets fiscales auditables y previene notas de crédito parciales complejas en la caja rápida.

### B. Máquina de Estados de la Orden y Motivos Tipados
- La entidad `Order` incorpora la transición hacia el estado inmutable `CANCELLED` (válida únicamente desde el estado `PAID`).
- Campos de trazabilidad en tabla `orders`:
  - `cancellation_reason`: Enum tipado obligatorio:
    1. `CUSTOMER_RETRACTED` (Cliente desiste / demora de entrega).
    2. `TYPING_ERROR` (Error de digitación o mesa equivocada del cajero).
    3. `KITCHEN_ISSUE` (Plato quemado / falta de insumo detectada en cocina).
    4. `QUALITY_COMPLAINT` (Reclamo de calidad / plato rechazado por el comensal).
    5. `DUPLICATE_ORDER` (Comanda duplicada por error).
  - `cancellation_notes`: Justificación textual libre opcional.
  - `cancelled_at`: Marca temporal de la cancelación (`TIMESTAMPTZ`).
  - `cancelled_by`: ID del usuario cajero que solicitó la operación.
  - `cancellation_authorized_by`: ID del usuario supervisor/administrador que autorizó.
  - `refund_method`: Canal de reintegro utilizado (`CASH` | `DIGITAL`).
  - `waste_policy_applied`: Destino asignado a los insumos (`CANCELLATION_REVERSAL`, `WASTE_ADJUSTMENT`, `BUFFER_REUSE`).

---

### C. Motor Unificado de Autorizaciones en Finanzas y Canal Dual (PIN Rápido)
En lugar de mantener aprobaciones dispersas para gastos y cancelaciones, se formaliza el modelo unificado dentro del módulo de Finanzas (`modules.finance`):
- **Entidad `ShiftAuthorization` (`shift_authorizations`):**
  - `id`, `tenant_id`, `branch_id`, `cash_shift_id` (opcional), `request_type` (`EXPENSE`, `ORDER_CANCELLATION`), `reference_id` (ID de la orden o del gasto), `requested_by`, `amount` (monto monetario), `status` (`PENDING`, `APPROVED`, `REJECTED`), `justification`, `decided_by`, `decision_notes`, `decision_channel` (`IN_SITU_PIN`, `REMOTE_SESSION`), `created_at`, `decided_at`.
- **Canal 1 — In-Situ mediante PIN Rápido (4 a 6 dígitos):**
  - Todo usuario con rol Supervisor o Administrador puede configurar su PIN numérico en "Mi Perfil" (`app_users.pin_hash` con BCrypt).
  - En la terminal del cajero, al pulsar "Cancelar Orden", se despliega un teclado numérico táctil. El supervisor ingresa su PIN: el backend verifica la firma y ejecuta la anulación de forma síncrona en una sola transacción sin necesidad de cambiar de sesión.
- **Canal 2 — Remoto / Asíncrono (Cola en `/finance`):**
  - Si el supervisor no está al lado de la caja, el cajero despacha la solicitud en cola con estado `PENDING`.
  - El supervisor visualiza la solicitud en la bandeja de autorizaciones y la aprueba o rechaza a distancia.
- **Gobernanza a Futuro (`PB-053`):** Se catalogó en el Product Backlog la tarea de baja prioridad para extraer este motor hacia `core.authorization` cuando surjan autorizaciones no financieras (compras de almacén o RRHH).

---

### D. Tratamiento de Kardex y Buffer de Vitrina (`pos.cancellation_prepared_waste_policy`)
Al publicarse `OrderCancelledEvent`, el listener de inventario (`KardexService`) evalúa el estado de elaboración y el parámetro de sucursal:

1. **Caso 1: NO Preparado en Cocina (`wasPrepared = false`):**
   - El plato no se cocinó. Los insumos regresan automáticamente al stock disponible en `branch_stock` con movimiento de Kardex tipo **`CANCELLATION_REVERSAL`**.
2. **Caso 2: SÍ Preparado en Cocina (`wasPrepared = true`):**
   - Se evalúa el parámetro de configuración multinivel:
     - **`ALLOW_AUTO_BUFFER_REUSE` (Predeterminado):** El plato se transfiere automáticamente al **Buffer de Vitrina / Platos Disponibles** (`prepared_dish_buffer`). No se asienta merma en Kardex. Cuando otro cliente pida ese plato, se despacha desde vitrina sin descontar insumos por segunda vez. Al cierre de turno, los platos sobrantes en vitrina se merman automáticamente.
     - **`ALLOW_BUFFER_REUSE`:** El modal solicita al supervisor decidir si el plato está apto para pasar a vitrina o si debe enviarse a merma directa.
     - **`STRICT_WASTE`:** El plato se destruye físicamente y se emite obligatoriamente un movimiento de Kardex tipo **`WASTE_ADJUSTMENT`** (Merma de Venta Cancelada) registrando la pérdida económica de la sucursal.
3. **Platos de Venta Libre (`FREE_SALE`):** No generan movimientos en Kardex en ningún caso.

---

### E. Devolución de Dinero en Caja (Efectivo vs. Digital)
En el diálogo de cancelación, se consulta explícitamente la vía acordada con el comensal:
- **`CASH` (Efectivo de Gaveta):**
  - Se genera automáticamente un egreso transaccional tipo `REFUND` en el turno activo (`cash_shifts.total_cash_refunds`), reduciendo el efectivo esperado en gaveta para que el arqueo físico cuadre con precisión matemática.
- **`DIGITAL` (Tarjeta / QR / Transferencia):**
  - Se registra la anulación contable del cobro digital sin alterar el saldo físico de billetes de la gaveta.

---

### F. Frontend: Componente Standalone Reutilizable `<app-authorization-queue>`
Siguiendo la regla de oro de prevención de *God Components*:
- Se construye un **Componente Presentacional Puro (*Dumb Component*)** en `shared/components/ui/authorization-queue/`:
  - Recibe datos mediante Signals: `authorizations = input.required()`, `viewMode = input<'CASHIER' | 'SUPERVISOR'>()`, `filterType = model('ALL')`.
  - Notifica eventos: `approve = output()`, `reject = output()`, `requestPin = output()`.
  - Cero inyecciones de servicios HTTP ni mutaciones de estado global en su interior.
- Se reutiliza idénticamente en:
  - **`/cash-control`:** Modo cajero (solo visualización de sus solicitudes y botón para invocar PIN).
  - **`/finance`:** Modo supervisor (bandeja completa con botones de acción `[Aprobar]` y `[Rechazar]`).

---

## 3. Consecuencias y Beneficios

1. **Cuadre de Caja Infalible:** Ningún efectivo sale de la gaveta sin su respectivo asiento `REFUND` debidamente autorizado.
2. **Control Real de Mermas:** El restaurante cuantifica en dinero cuánto pierde por cancelaciones y platos descartados en cocina.
3. **Agilidad en Fila:** El PIN táctil de 4-6 dígitos permite autorizaciones inmediatas en menos de 5 segundos.
4. **Cero Desperdicio Innecesario:** El buffer de vitrina permite recuperar el valor de platos preparados no despachados.
5. **Mantenibilidad Frontend:** La unificación en `<app-authorization-queue>` elimina duplicación masiva entre vistas.
