---
title: "Diagrama de Estados: Ciclo de Vida de Cancelaciones, Autorizaciones y Destino de Insumos"
category: "architecture"
level: "State"
module: "pos"
tags:
  - architecture
  - mermaid
  - state-machine
  - pos
  - cancellation
  - waste
  - buffer
last_updated: "2026-11-07"
status: "draft"
---

# 🔄 Diagrama de Estados: Ciclo de Vida de Cancelaciones, Autorizaciones y Destino de Insumos (PB-031)

Este diagrama documenta las transiciones de estado de una orden cancelada, las bifurcaciones de autorización y el ciclo de vida del inventario (reverso a Kardex vs buffer de vitrina vs merma física).

---

```mermaid
stateDiagram-v2
    [*] --> PAID: Orden Cobrada y Cerrada en POS

    state "Flujo de Cancelación" as CancellationFlow {
        PAID --> SolicitudCancelacion: Cajero solicita anulación (Motivo + Vía Devolución)
        
        state AutorizacionDecision <<choice>>
        SolicitudCancelacion --> AutorizacionDecision: Canal de Autorización

        AutorizacionDecision --> PENDING_APPROVAL: Canal Remoto (En cola para supervisor)
        AutorizacionDecision --> PIN_VALIDATION: Canal Rápido In-Situ (PIN táctil)

        PIN_VALIDATION --> CANCELLED: PIN válido de Supervisor
        PIN_VALIDATION --> SolicitudCancelacion: PIN incorrecto (Reintento)

        PENDING_APPROVAL --> CANCELLED: Supervisor aprueba en /finance
        PENDING_APPROVAL --> PAID: Supervisor rechaza solicitud
    }

    state "Tratamiento de Inventario" as InventoryFlow {
        state EstadoCocina <<choice>>
        CANCELLED --> EstadoCocina: ¿Plato fue elaborado en cocina?

        EstadoCocina --> CANCELLATION_REVERSAL: NO preparado (Insumos devueltos al almacén)
        EstadoCocina --> PoliticaPreparados: SÍ preparado en cocina

        state PoliticaPreparados <<choice>>
        PoliticaPreparados --> BUFFER_REUSE: ALLOW_AUTO_BUFFER_REUSE / Vitrina
        PoliticaPreparados --> WASTE_ADJUSTMENT: STRICT_WASTE / Merma

        BUFFER_REUSE --> REUSED_SALE: Próximo comensal pide el plato (Vendido sin re-deducir insumos)
        BUFFER_REUSE --> WASTE_ADJUSTMENT: Cierre de Turno / Plato expira en vitrina
    }

    CANCELLATION_REVERSAL --> [*]
    WASTE_ADJUSTMENT --> [*]
    REUSED_SALE --> [*]
```
