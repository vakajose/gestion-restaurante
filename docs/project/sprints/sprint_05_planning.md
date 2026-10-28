---
title: "Sprint 5 Planning: Medios Desacoplados, Catálogo y Personalización de Recetas"
category: "sprints"
sprint: 5
status: "planned"
last_updated: "2026-10-28"
---

# 📅 Sprint 5 Planning — Medios Desacoplados, Catálogo y Personalización de Recetas (Planificado)

> **Estado del Sprint:** `PLANIFICADO / PENDIENTE DE INICIO TRAS SPRINT 4`  
> **Objetivo del Sprint:** Arquitectura sin estado (stateless) para medios digitales con procesamiento automático WebP (S3/Local), galería de platos en POS con toggle de rendimiento, personalización de pedidos mediante exclusiones BOM de ingredientes y notas de cocina, y modelo de distribución centralizada con transferencias entre sucursales.

---

## Tareas Planificadas para Sprint 5

- [ ] **TASK-326 [STORAGE] [DISEÑO]:** Especificación del Motor Desacoplado de Almacenamiento y Optimización WebP de Medios (PB-035 / UC-326).
  - *Objetivo:* Diseñar contrato agnóstico `MediaStorageService` (soporte Local para desarrollo y S3/R2 para producción) y pipeline de compresión/reescalado automático a WebP (`120x120` miniaturas, `400x400` catálogo).
  - *Criterio de Entrega:* Especificación de arquitectura sin estado (stateless) para contenedores Docker.

- [ ] **TASK-327 [STORAGE] [ARQUITECTURA]:** Implementación de `MediaStorageService` con Resize Automático y Soporte S3/Local (PB-035).
  - *Objetivo:* Implementar servicio de medios, endpoints de carga multipart con validación MIME/tamaño y reutilización en gastos de caja chica (`expenses`) y catálogo.
  - *Criterio de Entrega:* Imágenes comprimidas a WebP < 100KB y compatibilidad transparente local / S3 mediante variables de entorno.

- [ ] **TASK-328 [CATALOG] [UI/UX]:** Visualización de Imágenes de Platos en Punto de Venta con Toggle Parametrizable (PB-036).
  - *Objetivo:* Cuadrícula visual en POS con miniaturas optimizadas y lazy-loading, fallback a iconos temáticos y botón toggle en cabecera gobernado por el parámetro `POS_DISPLAY_DISH_IMAGES`.
  - *Criterio de Entrega:* Interfaz de cobro visual fluida con opción de alternar a modo compacto sin recargar página.

- [ ] **TASK-329 [CATALOG] [DISEÑO]:** Especificación de Exclusión de Insumos BOM y Notas de Cocina en Comanda (PB-037 / UC-329).
  - *Objetivo:* Diseñar modal de personalización de plato en POS: exclusión de insumos ("Sin arroz") con impacto condicional en Kardex (`EXCLUSION_REDUCES_KARDEX_DEDUCTION`), y notas de cocina libres sin impacto en inventario.
  - *Criterio de Entrega:* Especificación de modelo de datos y flujo de eventos en `docs/project/architecture/`.

- [ ] **TASK-330 [CATALOG] [NEGOCIO]:** Implementación de Exclusiones de Receta en POS con Impacto Condicional en Kardex (PB-037).
  - *Objetivo:* Persistencia de `order_item_exclusions`, adaptación de `KardexService` ante `OrderPaidEvent` para descontar únicamente insumos no excluidos según parámetro activo, e impresión en ticket de comanda.
  - *Criterio de Entrega:* Comanda refleja exclusiones e inventario se deduce según la política parametrizada de la empresa.

- [ ] **TASK-331 [INVENTORY] [DISEÑO]:** Especificación del Modelo de Distribución de Inventario Centralizado vs Descentralizado (PB-033 / UC-331).
  - *Objetivo:* Diseñar la arquitectura del modelo de distribución parametrizable (`INVENTORY_DISTRIBUTION_MODEL`: `DIRECT_PER_BRANCH` vs `CENTRAL_WAREHOUSE_HUB`), definición de Almacén Central vs Sucursal Satélite, reglas de compras/recepciones restringidas y flujo de transferencias internas de stock (`StockTransfer`).
  - *Criterio de Entrega:* Documento SDD en `docs/project/architecture/` con ERD y diagrama de secuencia de transferencias y recepciones.

- [ ] **TASK-332 [INVENTORY] [ARQUITECTURA]:** Implementación de Modelo de Inventario Parametrizable y Transferencias entre Sucursales (PB-033 / PB-004).
  - *Objetivo:* Adaptar validaciones de compras para restringir recepciones directas en sucursales cuando el modelo centralizado esté activo, e implementar circuito de solicitud y despacho de transferencias internas entre almacenes con actualización atómica de Kardex.
  - *Criterio de Entrega:* Restricción de compras operando según parámetro de empresa y transferencias de stock registradas en Kardex de origen y destino.

- [ ] **TASK-227 [INVENTORY] [NEGOCIO]:** Módulo de Recepción de Compras a Proveedores (`PURCHASE`) y Ajustes de Merma / Conteo Físico (PB-021 - Diferido de Sprint 2).
  - *Objetivo:* Completar el ciclo de abastecimiento de almacén con registro de facturas/recibos, recálculo de costo ponderado móvil y control de mermas con conteo físico.
  - *Criterio de Entrega:* Recepción de compras incrementa stock físico y Kardex, y ajustes por conteo físico registran diferencias de inventario.
