# Diagrama de Secuencia: Invariante de Turno Abierto y Validación de Comandas en POS

Este diagrama describe la interacción modular entre el **Terminal POS**, el módulo **`modules.pos`**, el módulo **`modules.finance`** y la persistencia en base de datos PostgreSQL, implementando el **invariante financiero estricto** definido en el **ADR-0013**.

---

## Flujo de Cobro de Comanda con Verificación de Turno

```mermaid
sequenceDiagram
    autonumber
    actor C as Terminal POS (Web / Tablet / Móvil)
    participant POS as OrderServiceImpl (modules.pos)
    participant FIN as FinancePublicApi (modules.finance)
    participant DB as PostgreSQL (orders / order_items)
    participant BUS as Domain Event Bus (ApplicationEventPublisher)

    C->>POS: POST /api/v1/pos/orders (CreateOrderRequest)
    Note over POS: 1. Inspección de Contexto Multi-Tenant y Sucursal
    POS->>FIN: findActiveShift(tenantId, request.branchId())

    alt Caso A: Sucursal sin Turno Abierto (Caja Cerrada)
        FIN-->>POS: Optional.empty()
        Note over POS: 2. Violación de Invariante Financiero
        POS-->>C: 409 Conflict (RFC 7807: SHIFT_REQUIRED_ERROR)
        Note over C: Muestra Banner "Caja Cerrada" y botón CTA "Abrir Turno"
    else Caso B: Turno Activo Existente (Estado OPEN)
        FIN-->>POS: Optional.of(activeShift)
        Note over POS: 3. Vinculación Atómica de Turno y Cajero
        POS->>DB: INSERT INTO orders (tenant_id, branch_id, cash_shift_id, user_id, status = 'PAID', ...)
        POS->>DB: INSERT INTO order_items (...)
        
        Note over POS: 4. Emisión de Evento de Dominio
        POS->>BUS: publishEvent(OrderPaidEvent(orderId, tenantId, branchId, cashShiftId, totalAmount, ...))
        
        Note over POS: 5. Respuesta Enriquecida al Terminal
        POS-->>C: 201 Created (OrderDto con ticketNumber, cashierId = user_id, cashShiftId = activeShift.id)
        Note over C: Cierra modal de cobro y despliega Comprobante/Ticket
    end
```

---

## Flujo de Registro y Autorización de Gastos de Caja Menor (RBAC Puro)

```mermaid
sequenceDiagram
    autonumber
    actor Solicitante as Colaborador en Turno (Cajero / Supervisor)
    participant EXP as ExpenseService (modules.finance)
    participant CFG as ConfigurationPublicApi (modules.configuration)
    participant SEC as SecurityContextHolder (core.security)
    participant DB as PostgreSQL (expenses)

    Solicitante->>EXP: POST /api/v1/finance/expenses (CreateExpenseRequest)
    Note over EXP: 1. Verificación de Límite de Gaveta
    EXP->>CFG: resolveParameter("finance.max_petty_cash_expense")
    CFG-->>EXP: 100.00 BOB

    alt Monto supera techo de gaveta (> 100.00) y paidFromCashDrawer == true
        EXP-->>Solicitante: 400 Bad Request ("Supera límite máximo de caja menor")
    else Monto válido (<= 100.00)
        Note over EXP: 2. Evaluación de Permisos RBAC
        EXP->>SEC: hasAuthority("finance:expenses_approve")
        
        alt Solicitante NO tiene permiso de aprobación (ej. Cajero)
            Note over EXP: Solicitud Regular
            EXP->>DB: INSERT INTO expenses (status = 'PENDING_APPROVAL', approved_by = null)
            EXP-->>Solicitante: 201 Created (ExpenseDto: PENDING_APPROVAL)
        else Solicitante TIENE permiso de aprobación (ej. Supervisor / Admin)
            EXP->>CFG: resolveParameter("finance.expense_allow_self_approval")
            CFG-->>EXP: false (Default)
            
            alt Es Administrador General (ADMIN_TENANT)
                EXP->>DB: INSERT INTO expenses (status = 'APPROVED', approved_by = userId)
                EXP-->>Solicitante: 201 Created (ExpenseDto: APPROVED)
            else Es Supervisor y self_approval == false
                EXP->>DB: INSERT INTO expenses (status = 'PENDING_APPROVAL', approved_by = null)
                EXP-->>Solicitante: 201 Created (ExpenseDto: PENDING_APPROVAL - Requiere control dual)
            else Es Supervisor y self_approval == true
                EXP->>DB: INSERT INTO expenses (status = 'APPROVED', approved_by = userId)
                EXP-->>Solicitante: 201 Created (ExpenseDto: APPROVED)
            end
        end
    end
```
