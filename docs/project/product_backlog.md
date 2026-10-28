---
title: "Product Backlog y Registro de Nuevas Funcionalidades"
category: "product-backlog"
tags:
  - backlog
  - roadmap
  - requirements
  - priorities
  - sdd
last_updated: "2026-10-28"
status: "verified"
---

# 📋 Product Backlog y Registro de Nuevas Funcionalidades

> 💡 **Nota de Optimización para Agentes y Desarrolladores:**
> Para optimizar el consumo de tokens y facilitar la navegación, el detalle exhaustivo de los requerimientos se encuentra modularizado en archivos independientes por nivel de criticidad en la carpeta [`product_backlog/`](./product_backlog/):
> - 🔴 [**`00_prioridad_p0_critica.md`**](./product_backlog/00_prioridad_p0_critica.md): Requerimientos fundacionales y de arquitectura multi-tenant crítica.
> - 🟠 [**`01_prioridad_p1_alta.md`**](./product_backlog/01_prioridad_p1_alta.md): Operaciones clave (POS, turnos, finanzas, inventario, sesiones, roles dinámicos).
> - 🟡 [**`02_prioridad_p2_media.md`**](./product_backlog/02_prioridad_p2_media.md): Mejoras de experiencia visual, hardware comanderas y tour interactivo.
> - 🟢 [**`03_prioridad_p3_baja_futuro.md`**](./product_backlog/03_prioridad_p3_baja_futuro.md): Módulos complementarios y extensiones futuras (salón, KDS, reservas).
> - 📑 [**`README.md del Backlog`**](./product_backlog/README.md): Índice central con la matriz de priorización detallada.

---

## 🧭 Índice Rápido del Product Backlog

| ID | Módulo / Tipo | Título del Requerimiento | Prioridad | Estado / Sprint | Acceso Directo |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **PB-001** | `[GLOBAL] [NEGOCIO]` | Onboarding del Cliente y Alta Rápida de Empresa | **P0** | Sprint 3 (En Progreso) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-001) |
| **PB-002** | `[GLOBAL] [SEGURIDAD]` | Rol "Árbitro del Sistema" (Superadmin SaaS) | **P0** | En Backlog | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-002) |
| **PB-003** | `[GLOBAL] [ARQUITECTURA]` | Backoffice Independiente (Frontend + BFF) | **P0** | En Backlog | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-003) |
| **PB-017** | `[COMPANY] [NEGOCIO]` | Gestión de Empresa (Tenant) y Red de Sucursales | **P0** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-017) |
| **PB-018** | `[AUTH] [SEGURIDAD]` | RBAC y Gestión de Usuarios por Tenant | **P0** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-018) |
| **PB-019** | `[AUTH] [SEGURIDAD]` | Autenticación por Tenant Identifier y Selector Sucursal | **P0** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-019) |
| **PB-022** | `[DEVOPS] [CALIDAD]` | Corrección de Suite de Pruebas Frontend en CI | **P0** | Completado (TASK-011) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-022) |
| **PB-023** | `[DOCS] [ARQUITECTURA]` | Bóveda de Arquitectura Viva en Mermaid (SDD) | **P0** | Completado (TASK-012..017) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-023) |
| **PB-026** | `[CONFIG] [ARQUITECTURA]` | Parametrización Multinivel con Hot-Reload | **P0** | Sprint 3 (En Progreso) | [Ver detalle](./product_backlog/00_prioridad_p0_critica.md#pb-026) |
| **PB-004** | `[INVENTORY] [NEGOCIO]` | Transferencias de Insumos entre Sucursales | **P1** | Sprint 5 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-004) |
| **PB-005** | `[FINANCE] [BI]` | Consolidación Financiera Multi-Sucursal | **P1** | En Backlog | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-005) |
| **PB-011** | `[GLOBAL] [ARQUITECTURA]` | Desacoplamiento de Repositorios a APIs Públicas | **P1** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-011) |
| **PB-012** | `[GLOBAL] [ARQUITECTURA]` | Excepciones de Dominio y ProblemDetail RFC 7807 | **P1** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-012) |
| **PB-016** | `[POS] [ARQUITECTURA]` | Repositorios Locales DAL para Dexie.js | **P1** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-016) |
| **PB-020** | `[POS] [NEGOCIO]` | Motor de Promociones, Combos y Modificadores | **P1** | Sprint 2 (Completado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-020) |
| **PB-021** | `[INVENTORY] [NEGOCIO]` | Inicialización de Stock, Compras y Mermas | **P1** | Sprint 2 (Parcial) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-021) |
| **PB-024** | `[FINANCE] [BI]` | Panel de Analítica y Rentabilidad Neta | **P1** | Pospuesto | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-024) |
| **PB-025** | `[DEVOPS] [INFRA]` | Despliegue, Contenerización Docker y Cloud | **P1** | En Backlog (DevOps) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-025) |
| **PB-027** | `[AUTH] [SEGURIDAD]` | Ciclo de Vida de Sesiones (JWT Refresh & Timeout) | **P1** | Sprint 3 (Por Iniciar) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-027) |
| **PB-028** | `[UI/UX] [ARQUITECTURA]` | Diseño Responsivo y Navegación Móvil | **P1** | Sprint 3 (En Progreso) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-028) |
| **PB-029** | `[FINANCE] [NEGOCIO]` | Visor de Ventas del Turno Activo para Cajero | **P1** | Sprint 4 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-029) |
| **PB-030** | `[FINANCE] [BI]` | Histórico Gerencial de Turnos y Auditoría de Ventas | **P1** | Sprint 4 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-030) |
| **PB-031** | `[POS] [NEGOCIO]` | Cancelación de Pedidos, Devolución y Kardex | **P1** | Sprint 4 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-031) |
| **PB-032** | `[POS] [NEGOCIO]` | Pedidos "Para Llevar" (Takeout) y Delivery | **P1** | Sprint 4 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-032) |
| **PB-033** | `[INVENTORY] [ARQUITECTURA]` | Modelo de Inventario Centralizado vs Sucursal | **P1** | Sprint 5 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-033) |
| **PB-034** | `[POS] [NEGOCIO]` | Apertura de Turno con Menú y Cupos de Platos | **P1** | Sprint 4 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-034) |
| **PB-035** | `[STORAGE] [ARQUITECTURA]` | Almacenamiento Desacoplado y WebP (S3/Local) | **P1** | Sprint 5 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-035) |
| **PB-037** | `[CATALOG] [NEGOCIO]` | Exclusión de Insumos BOM y Notas de Cocina | **P1** | Sprint 5 (Planificado) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-037) |
| **PB-039** | `[AUTH] [SEGURIDAD]` | Roles Personalizados, Privilegios y Matriz RBAC | **P1** | Sprint 3 (Próximo en Diseñar) | [Ver detalle](./product_backlog/01_prioridad_p1_alta.md#pb-039) |
| **PB-006** | `[CORE] [ARQUITECTURA]` | Desacoplamiento Multi-Motor SQL | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-006) |
| **PB-007** | `[POS] [NEGOCIO]` | Facturación Electrónica Oficial | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-007) |
| **PB-008** | `[POS] [INFRA]` | Impresión Física ESC/POS (Comanderas de Red) | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-008) |
| **PB-013** | `[CORE] [ARQUITECTURA]` | Evolución a Modelo de Dominio Rico | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-013) |
| **PB-014** | `[INVENTORY] [ARQUITECTURA]` | Strategy Pattern Dinámico en Kardex | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-014) |
| **PB-015** | `[POS] [ARQUITECTURA]` | Secuencia Diaria de Comandas Desacoplada | **P2** | En Backlog | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-015) |
| **PB-036** | `[CATALOG] [UI/UX]` | Catálogo Visual con Imágenes de Platos en POS | **P2** | Sprint 5 (Planificado) | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-036) |
| **PB-040** | `[UI/UX] [NEGOCIO]` | Tour Guiado Interactivo (Product Tour Onboarding) | **P2** | Sprint 3 (Planificado) | [Ver detalle](./product_backlog/02_prioridad_p2_media.md#pb-040) |
| **PB-009** | `[SALON] [UI/UX]` | Mapa Visual Interactivo de Mesas | **P3** | En Backlog | [Ver detalle](./product_backlog/03_prioridad_p3_baja_futuro.md#pb-009) |
| **PB-010** | `[POS] [UI/UX]` | Panel KDS Táctil con Métricas de Despacho | **P3** | En Backlog | [Ver detalle](./product_backlog/03_prioridad_p3_baja_futuro.md#pb-010) |
| **PB-038** | `[SALON] [NEGOCIO]` | Gestión de Reservas y Capacidad de Salón | **P3** | En Backlog | [Ver detalle](./product_backlog/03_prioridad_p3_baja_futuro.md#pb-038) |
