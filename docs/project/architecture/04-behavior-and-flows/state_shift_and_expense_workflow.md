---
title: "Diagrama de Estados y Flujo Operativo: Ciclo de Turno de Caja y Gestión de Gastos"
category: "architecture"
level: "State"
module: "finance"
tags:
  - architecture
  - mermaid
  - sdd
  - state
  - flowchart
  - finance
  - cash-shift
  - expenses
  - arqueo
last_updated: "2026-10-25"
status: "verified"
---

# 💰 Diagrama de Estados y Flujo Operativo: Ciclo de Turno de Caja y Gestión de Gastos

Este documento modela la arquitectura operativa y matemática del módulo de **Finanzas**: el ciclo de vida del **Turno de Caja** (`CashShift`), el flujo de aprobación de **Gastos Menores / Caja Chica** (`Expense`) y la fórmula del **Arqueo de Caja** para el balance y control de descuadres en las sucursales.

---

## 🧭 1. Resumen Ejecutivo del Módulo Financiero

El sistema implementa un modelo estricto de control de efectivo diseñado para evitar pérdidas, robos hormiga y discrepancias operativas:
1. **Unicidad de Turno Abierto:** En una sucursal dada (`branch_id`), únicamente puede existir **un solo turno de caja abierto a la vez** (`status = 'OPEN'`).
2. **Fondo de Inicio de Gaveta (`initial_cash`):** El cajero declara el efectivo base con el que arranca operaciones para dar cambio.
3. **Acumulación Asíncrona en Vivo:** A través de `OrderPaidEvent` y `FinanceEventListener`, las ventas cobradas se suman automáticamente en tiempo real en los acumuladores del turno: `total_cash_sales`, `total_card_sales` y `total_other_sales`.
4. **Gobierno de Gastos de Caja Chica (`expenses`):** Cualquier salida imprevista de dinero de la gaveta física (compra de hielo, propinas a proveedores, artículos de limpieza) requiere aprobación jerárquica del encargado antes de computarse en el arqueo.
5. **Cierre de Turno y Arqueo Ciego:** Al culminar la jornada, el cajero realiza el conteo físico de los billetes y monedas (`actual_cash`). El sistema contrasta este valor contra el cálculo matemático estricto del sistema (`expected_cash`), determinando la diferencia exacta (`difference`: faltante o sobrante).

---

## 📊 2. Diagramas Mermaid

### 2.1. Máquina de Estados: Turno de Caja y Gastos Menores

```mermaid
stateDiagram-v2
    direction TB

    %% -------------------------------------------------------------
    %% SUB-MAQUINA 1: CICLO DE VIDA DEL TURNO DE CAJA (CashShift)
    %% -------------------------------------------------------------
    state "Turno de Caja (CashShift)" as ShiftFlow {
        [*] --> OPEN: Cajero realiza OpenShiftRequest<br/>{Declara initial_cash, valida que no haya otro turno activo}
        
        state OPEN {
            [*] --> OperandoTurno
            OperandoTurno --> OperandoTurno: Acumula ventas vía OrderPaidEvent<br/>(CASH, CARD, QR, TRANSFER)
            OperandoTurno --> OperandoTurno: Registra egresos de caja chica
        }

        OPEN --> CLOSED: Cajero ejecuta CloseShiftRequest<br/>{Declara actual_cash físico, calcula arqueo, sella turno}
        CLOSED --> [*]: Turno Inmutable (Auditoría finalizada)
    }

    %% -------------------------------------------------------------
    %% SUB-MAQUINA 2: CICLO DE VIDA DEL GASTO MENOR (Expense)
    %% -------------------------------------------------------------
    state "Gasto Menor / Caja Chica (Expense)" as ExpenseFlow {
        [*] --> SolicitudGasto: Cajero solicita egreso (paidFromCashDrawer = true)
        
        SolicitudGasto --> PENDING_APPROVAL: Creado por CASHIER<br/>{Adjunta evidencia digital/recibo}
        SolicitudGasto --> APPROVED: Creado por BRANCH_MANAGER o ADMIN_TENANT<br/>(Aprobación automática por jerarquía)
        
        PENDING_APPROVAL --> APPROVED: Encargado aprueba egreso<br/>[Actor: BRANCH_MANAGER]<br/>{Impacta expected_cash en el arqueo}
        PENDING_APPROVAL --> REJECTED: Encargado rechaza egreso<br/>[Actor: BRANCH_MANAGER]<br/>{NO descuenta efectivo de la gaveta}

        APPROVED --> [*]: Gasto computado en egresos
        REJECTED --> [*]: Gasto desestimado
    }
```

---

### 2.2. Flujograma del Arqueo y Conciliación Matemática

```mermaid
flowchart TD
    Start(["Inicio: Cierre de Turno de Caja"]) --> ReadShift["1. Obtener Turno Activo<br/>(initial_cash declarado en apertura)"]
    
    ReadShift --> QuerySales["2. Consultar Ventas Consolidadas en POS<br/>(PosPublicApi.getShiftSalesSummary)"]
    QuerySales --> SumMethods["Desglosar Ventas:<br/>- totalCashSales (Efectivo)<br/>- totalCardSales (Tarjetas)<br/>- totalOtherSales (QR / Transferencias)"]
    
    SumMethods --> QueryExpenses["3. Consultar Gastos de Caja Chica en BD<br/>(paid_from_cash_drawer = true AND approval_status = 'APPROVED')"]
    QueryExpenses --> SumExpenses["Calcular totalCashExpenses:<br/>sum(monto de gastos aprobados de gaveta)"]

    SumExpenses --> CalcExpected["4. Calcular Efectivo Esperado en Gaveta<br/><b>expected_cash = initial_cash + totalCashSales - totalCashExpenses</b>"]
    
    CalcExpected --> InputActual["5. Cajero ingresa Efectivo Real Contado Físicamente<br/>(actual_cash)"]
    
    InputActual --> CalcDiff["6. Calcular Diferencia de Arqueo<br/><b>difference = actual_cash - expected_cash</b>"]

    CalcDiff --> CheckDiff{"Evaluar Diferencia"}
    
    CheckDiff -->|difference == 0| Square["✅ Caja Perfectamente Cuadrada<br/>(Sin discrepancias)"]
    CheckDiff -->|difference > 0| Surplus["⚠️ Sobrante de Caja<br/>(Dinero extra no facturado o vuelto no entregado)"]
    CheckDiff -->|difference < 0| Deficit["❌ Faltante de Caja (Descuadre)<br/>(Falta dinero en gaveta; cargo al cajero)"]

    Square --> Finalize["7. Guardar en Base de Datos (CashShift):<br/>- status = 'CLOSED'<br/>- closed_by = userId<br/>- closed_at = Instant.now()<br/>- Congela expected_cash, actual_cash y difference"]
    Surplus --> Finalize
    Deficit --> Finalize

    Finalize --> End(["Fin: Turno Cerrado e Inmutable"])
```

---

## 🧮 3. Fundamento Matemático del Arqueo de Caja

La conciliación del arqueo de caja se rige por la fórmula canónica del sistema:

### 3.1. Fórmula del Efectivo Esperado (`expected_cash`)
$$\text{expected\_cash} = \text{initial\_cash} + \text{total\_cash\_sales} - \text{total\_cash\_expenses}$$

Donde:
- $\text{initial\_cash}$: Monto base entregado al cajero al abrir el turno (`cash_shifts.initial_cash`).
- $\text{total\_cash\_sales}$: Sumatoria de todas las órdenes en estado `PAID` con método de pago `CASH` procesadas durante el turno.
- $\text{total\_cash\_expenses}$: Sumatoria de egresos que cumplen estrictamente:
  $$\text{total\_cash\_expenses} = \sum_{e \in E} e.\text{amount} \quad \text{donde} \quad E = \{ e \mid e.\text{paid\_from\_cash\_drawer} = \text{true} \land e.\text{approval\_status} = \text{'APPROVED'} \}$$

> [!IMPORTANT]
> Los gastos con estado `PENDING_APPROVAL` o `REJECTED` **no se restan del efectivo esperado**. Si un cajero saca dinero de la gaveta para un gasto pero este no fue aprobado por el encargado, el arqueo detectará un faltante.
> De igual forma, los gastos donde `paid_from_cash_drawer = false` (pagados por transferencia corporativa o cuenta corriente de la empresa) **no afectan la gaveta física**.

### 3.2. Fórmula de la Diferencia (`difference`)
$$\text{difference} = \text{actual\_cash} - \text{expected\_cash}$$

- **Caso 1: $\text{difference} = 0.00$:** El conteo físico coincide al céntimo con las operaciones registradas.
- **Caso 2: $\text{difference} > 0.00$ (Sobrante):** Existe más dinero físico en la gaveta del que el sistema calculó. Común en propinas retenidas por error o cambio no entregado a clientes.
- **Caso 3: $\text{difference} < 0.00$ (Faltante / Descuadre):** Hay menos dinero físico en la gaveta del que debería haber según las ventas y gastos aprobados. El cajero es financieramente responsable por el importe faltante.

---

## 🛡️ 4. Política de Aprobación de Gastos Menores

Para salvaguardar la caja chica y evitar extracciones no autorizadas, el sistema aplica la siguiente matriz de gobierno en `ExpenseService`:

```text
+-----------------------+------------------------+-------------------+--------------------+------------------------+
| Rol del Solicitante   | paid_from_cash_drawer  | Estado Inicial    | Requiere Revisión? | Afecta Arqueo?         |
+-----------------------+------------------------+-------------------+--------------------+------------------------+
| CASHIER               | true (Caja Chica)      | PENDING_APPROVAL  | SÍ (BRANCH_MANAGER)| Solo tras ser APPROVED |
| CASHIER               | false (Cuenta Empresa) | APPROVED          | NO                 | NO (No toca gaveta)    |
| BRANCH_MANAGER        | true (Caja Chica)      | APPROVED          | NO (Auto-aprobado) | SÍ (Inmediato)         |
| ADMIN_TENANT          | true o false           | APPROVED          | NO (Auto-aprobado) | SÍ (Si es gaveta)      |
+-----------------------+------------------------+-------------------+--------------------+------------------------+
```

### Reglas de Auditoría en la Aprobación:
- El encargado (`BRANCH_MANAGER`) revisa la descripción, el importe y el enlace a la foto/recibo digital (`evidence_url`).
- Al resolver el gasto mediante `approveExpense`, se guardan de forma permanente:
  - `approval_status`: `'APPROVED'` o `'REJECTED'`.
  - `approved_by`: UUID del encargado que tomó la decisión.
  - `approval_notes`: Justificación de la aprobación o motivo del rechazo.
- Un gasto en estado `APPROVED` o `REJECTED` se vuelve **inmutable**. No puede volver a procesarse ni modificarse.

---

## 🔗 Enlaces Cruzados a Bóveda y Documentación

- **Diagrama de Secuencia POS a Finanzas:** [sequence_pos_order_deduction.md](./sequence_pos_order_deduction.md)
- **Ciclo de Vida de Comandas:** [state_order_lifecycle.md](./state_order_lifecycle.md)
- **Casos de Uso de Finanzas:** [use_cases_finance.md](../03-use-cases/use_cases_finance.md)
- **ERD PostgreSQL (Tablas `cash_shifts` y `expenses`):** [database_erd.md](../02-domain-and-data/database_erd.md)
- **ADR-0006: Zonas Horarias y Manejo de Fechas:** [docs/adr/0006-strict-datetime-and-timezone-handling.md](../../../adr/)
