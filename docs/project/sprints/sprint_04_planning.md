---
title: "Sprint 4 Planning: Flujo Operativo de Ventas, Turnos y Finanzas"
category: "sprints"
sprint: 4
status: "planned"
last_updated: "2026-10-28"
---

# 📅 Sprint 4 Planning — Flujo Operativo de Ventas, Turnos y Finanzas (Planificado)

> **Estado del Sprint:** `PLANIFICADO / PENDIENTE DE INICIO TRAS SPRINT 3`  
> **Objetivo del Sprint:** Fortalecer el circuito operativo diario en sala y caja: auditoría de ventas del turno en vivo para cajeros, reportería histórica gerencial, workflow de cancelaciones y devoluciones con reversión a Kardex vs merma, modalidades para llevar (*Takeout*) y entrega, y control predictivo de cupos de menú en apertura de turno.

---

## Tareas Planificadas para Sprint 4

- [ ] **TASK-207 [POS] [NEGOCIO]:** Cupones de Descuento y Descuentos Manuales por Rol en Comanda (PB-020 / UC-207 - Diferido de Sprint 2).
  - *Objetivo:* Habilitar descuentos manuales por porcentaje o monto fijo en comanda, validando topes máximos por rol y soporte de cupones de descuento alfanuméricos.
  - *Criterio de Entrega:* Modal en POS para aplicar cupón o descuento por rol y persistencia en la orden con recálculo dinámico de totales.

- [ ] **TASK-318 [FINANCE] [NEGOCIO]:** Pestaña de Ventas y Comandas del Turno Activo para el Cajero en Finanzas (PB-029 / UC-318).
  - *Objetivo:* Diseñar e implementar nueva pestaña "Ventas del Turno" en `/finance`: listado en tiempo real de órdenes cobradas en el turno activo con desglose por medio de pago (`CASH`, `CARD`, `QR`) y modal de detalle de ticket.
  - *Criterio de Entrega:* El cajero audita todas sus ventas de la sesión y los totales cuadran con el cálculo teórico de arqueo.

- [ ] **TASK-319 [FINANCE] [BI]:** Vista de Histórico de Sesiones de Caja y Reporte de Ventas Multicriterio para Administrador (PB-030 / UC-319).
  - *Objetivo:* Diseñar e implementar panel de consulta histórica de turnos cerrados con filtros por rango de fechas (respetando huso local según ADR-0006), sucursal, cajero y detalle de todas las ventas y egresos de cada sesión.
  - *Criterio de Entrega:* Auditoría gerencial completa con exportación de resumen de turno.

- [ ] **TASK-320 [POS] [DISEÑO]:** Especificación del Flujo de Cancelación de Pedidos, Devoluciones y Tratamiento en Kardex (PB-031 / UC-320).
  - *Objetivo:* Diseñar máquina de estados para comanda `CANCELLED`, flujo de autorización por supervisor/PIN, registro financiero de devolución en turno de caja y regla de inventario: reintegro si no se cocinó vs merma/pérdida (`WASTE_ADJUSTMENT`) si ya se preparó.
  - *Criterio de Entrega:* Diagrama de estados y secuencia SDD formalizado en `docs/project/architecture/`.

- [ ] **TASK-321 [POS] [NEGOCIO]:** Implementación del Workflow de Cancelación con Autorización, Reintegro a Caja y Kardex (PB-031).
  - *Objetivo:* Endpoints de cancelación de orden, evento `OrderCancelledEvent`, listeners en Kardex y Finanzas, y modal de cancelación interactivo en frontend.
  - *Criterio de Entrega:* Cancelación revierte insumos o genera merma según estado de cocina y registra egreso de caja para arqueo exacto.

- [ ] **TASK-322 [POS] [DISEÑO]:** Especificación de Modalidades de Pedido: Para Llevar (Takeout), Encargos y Delivery (PB-032 / UC-322).
  - *Objetivo:* Diseñar tipos de servicio (`DINE_IN`, `TAKEOUT`, `SCHEDULED_PICKUP`, `DELIVERY`), datos complementarios (cliente, teléfono, hora pactada de entrega) y representación en tickets de comanda.
  - *Criterio de Entrega:* Especificación funcional y contrato de DTOs en `docs/project/architecture/`.

- [ ] **TASK-323 [POS] [NEGOCIO]:** Implementación de Pedidos Para Llevar, Distintivos en Comandas y Control de Despacho (PB-032).
  - *Objetivo:* Selector de modalidad en POS, persistencia en `orders`, distintivo visual prominente en ticket impreso y filtro de despacho en listado de pedidos.
  - *Criterio de Entrega:* Conmutación fluida en POS entre consumo local y para llevar con persistencia completa.

- [ ] **TASK-324 [POS] [DISEÑO]:** Especificación de Apertura Extendida de Turno con Selección de Menú y Cupos de Platos (PB-034 / UC-324).
  - *Objetivo:* Diseñar flujo opcional en apertura de turno: selección de platos activos en la sesión, definición de cupos máximos de preparación y algoritmo de verificación predictiva contra el stock en `branch_stock`.
  - *Criterio de Entrega:* Diagrama de actividad y contrato API de validación de insumos pre-apertura.

- [ ] **TASK-325 [POS] [NEGOCIO]:** Implementación de Asignación de Platos por Turno, Alerta de Insumos y Agotado en POS (PB-034).
  - *Objetivo:* Persistencia de `shift_menu_overrides`, endpoint de alerta de insumos insuficientes al abrir turno, y bloqueo reactivo de plato agotado en POS al copar el cupo.
  - *Criterio de Entrega:* Alerta preventiva en apertura de turno y bloqueo automático de venta al llegar al cupo pactado.
