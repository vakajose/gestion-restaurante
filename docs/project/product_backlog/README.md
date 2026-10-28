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

Este documento constituye el **Índice Central y Matriz de Priorización** del Product Backlog del Sistema de Gestión de Restaurante (POS + ERP).

Para optimizar la consulta por parte de desarrolladores y agentes de IA y evitar sobrecarga de contexto, el backlog se encuentra modularizado por nivel de criticidad en archivos independientes:

| Nivel de Prioridad | Enfoque Operativo | Archivo de Especificación |
| :--- | :--- | :--- |
| 🔴 **P0 (Máxima Prioridad / Crítica)** | Aislamiento, Onboarding, RBAC, Parametría Core y Estabilidad CI | [**`00_prioridad_p0_critica.md`**](./00_prioridad_p0_critica.md) |
| 🟠 **P1 (Alta Prioridad)** | Operación POS, Finanzas, Kardex, Sesiones, Roles y Móvil | [**`01_prioridad_p1_alta.md`**](./01_prioridad_p1_alta.md) |
| 🟡 **P2 (Media Prioridad)** | Hardware ESC/POS, Facturación, Imágenes de Menú y Tour | [**`02_prioridad_p2_media.md`**](./02_prioridad_p2_media.md) |
| 🟢 **P3 (Baja / Futuro)** | Salón Interactivo, KDS Cocina y Reservas de Mesas | [**`03_prioridad_p3_baja_futuro.md`**](./03_prioridad_p3_baja_futuro.md) |

---

## 🧭 Matriz de Priorización de Negocio

El backlog se gobierna mediante la siguiente escala:
1. **P0 (Máxima Prioridad):** Requerimientos indispensables para la arquitectura multi-inquilino, seguridad fundacional y ciclo de vida de la empresa.
2. **P1 (Alta Prioridad):** Funcionalidades requeridas para la operación diaria fluida del restaurante y solicitadas por el cliente potencial en Sprint 3.
3. **P2 (Media Prioridad):** Integraciones de hardware, optimizaciones de diseño y ampliaciones de catálogo.
4. **P3 (Baja Prioridad / Futuro):** Extensiones para etapas de escalamiento comercial maduro.

---

## 📑 Tabla Maestra de Requerimientos y Estado

| ID | Módulo / Tipo | Título del Requerimiento | Prioridad | Estado / Sprint | Archivo |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **PB-001** | `[GLOBAL] [NEGOCIO]` | Onboarding del Cliente y Alta Rápida de Empresa | **P0** | Sprint 3 (En Progreso) | [Ver en P0](./00_prioridad_p0_critica.md#pb-001) |
| **PB-002** | `[GLOBAL] [SEGURIDAD]` | Rol "Árbitro del Sistema" (Superadmin SaaS) | **P0** | En Backlog | [Ver en P0](./00_prioridad_p0_critica.md#pb-002) |
| **PB-003** | `[GLOBAL] [ARQUITECTURA]` | Backoffice Independiente (Frontend + BFF) | **P0** | En Backlog | [Ver en P0](./00_prioridad_p0_critica.md#pb-003) |
| **PB-017** | `[COMPANY] [NEGOCIO]` | Gestión de Empresa (Tenant) y Red de Sucursales | **P0** | Sprint 2 (Completado) | [Ver en P0](./00_prioridad_p0_critica.md#pb-017) |
| **PB-018** | `[AUTH] [SEGURIDAD]` | RBAC y Gestión de Usuarios por Tenant | **P0** | Sprint 2 (Completado) | [Ver en P0](./00_prioridad_p0_critica.md#pb-018) |
| **PB-019** | `[AUTH] [SEGURIDAD]` | Autenticación por Tenant Identifier y Selector Sucursal | **P0** | Sprint 2 (Completado) | [Ver en P0](./00_prioridad_p0_critica.md#pb-019) |
| **PB-022** | `[DEVOPS] [CALIDAD]` | Corrección de Suite de Pruebas Frontend en CI | **P0** | Completado (TASK-011) | [Ver en P0](./00_prioridad_p0_critica.md#pb-022) |
| **PB-023** | `[DOCS] [ARQUITECTURA]` | Bóveda de Arquitectura Viva en Mermaid (SDD) | **P0** | Completado (TASK-012..017) | [Ver en P0](./00_prioridad_p0_critica.md#pb-023) |
| **PB-026** | `[CONFIG] [ARQUITECTURA]` | Parametrización Multinivel con Hot-Reload | **P0** | Sprint 3 (En Progreso) | [Ver en P0](./00_prioridad_p0_critica.md#pb-026) |
| **PB-004** | `[INVENTORY] [NEGOCIO]` | Transferencias de Insumos entre Sucursales | **P1** | Sprint 5 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-004) |
| **PB-005** | `[FINANCE] [BI]` | Consolidación Financiera Multi-Sucursal | **P1** | En Backlog | [Ver en P1](./01_prioridad_p1_alta.md#pb-005) |
| **PB-011** | `[GLOBAL] [ARQUITECTURA]` | Desacoplamiento de Repositorios a APIs Públicas | **P1** | Sprint 2 (Completado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-011) |
| **PB-012** | `[GLOBAL] [ARQUITECTURA]` | Excepciones de Dominio y ProblemDetail RFC 7807 | **P1** | Sprint 2 (Completado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-012) |
| **PB-016** | `[POS] [ARQUITECTURA]` | Repositorios Locales DAL para Dexie.js | **P1** | Sprint 2 (Completado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-016) |
| **PB-020** | `[POS] [NEGOCIO]` | Motor de Promociones, Combos y Modificadores | **P1** | Sprint 2 (Completado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-020) |
| **PB-021** | `[INVENTORY] [NEGOCIO]` | Inicialización de Stock, Compras y Mermas | **P1** | Sprint 2 (Parcial) | [Ver en P1](./01_prioridad_p1_alta.md#pb-021) |
| **PB-024** | `[FINANCE] [BI]` | Panel de Analítica y Rentabilidad Neta | **P1** | Pospuesto | [Ver en P1](./01_prioridad_p1_alta.md#pb-024) |
| **PB-025** | `[DEVOPS] [INFRA]` | Despliegue, Contenerización Docker y Cloud | **P1** | En Backlog (DevOps) | [Ver en P1](./01_prioridad_p1_alta.md#pb-025) |
| **PB-027** | `[AUTH] [SEGURIDAD]` | Ciclo de Vida de Sesiones (JWT Refresh & Timeout) | **P1** | Sprint 3 (Por Iniciar) | [Ver en P1](./01_prioridad_p1_alta.md#pb-027) |
| **PB-028** | `[UI/UX] [ARQUITECTURA]` | Diseño Responsivo y Navegación Móvil | **P1** | Sprint 3 (En Progreso) | [Ver en P1](./01_prioridad_p1_alta.md#pb-028) |
| **PB-029** | `[FINANCE] [NEGOCIO]` | Visor de Ventas del Turno Activo para Cajero | **P1** | Sprint 4 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-029) |
| **PB-030** | `[FINANCE] [BI]` | Histórico Gerencial de Turnos y Auditoría de Ventas | **P1** | Sprint 4 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-030) |
| **PB-031** | `[POS] [NEGOCIO]` | Cancelación de Pedidos, Devolución y Kardex | **P1** | Sprint 4 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-031) |
| **PB-032** | `[POS] [NEGOCIO]` | Pedidos "Para Llevar" (Takeout) y Delivery | **P1** | Sprint 4 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-032) |
| **PB-033** | `[INVENTORY] [ARQUITECTURA]` | Modelo de Inventario Centralizado vs Sucursal | **P1** | Sprint 5 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-033) |
| **PB-034** | `[POS] [NEGOCIO]` | Apertura de Turno con Menú y Cupos de Platos | **P1** | Sprint 4 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-034) |
| **PB-035** | `[STORAGE] [ARQUITECTURA]` | Almacenamiento Desacoplado y WebP (S3/Local) | **P1** | Sprint 5 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-035) |
| **PB-037** | `[CATALOG] [NEGOCIO]` | Exclusión de Insumos BOM y Notas de Cocina | **P1** | Sprint 5 (Planificado) | [Ver en P1](./01_prioridad_p1_alta.md#pb-037) |
| **PB-039** | `[AUTH] [SEGURIDAD]` | Roles Personalizados, Privilegios y Matriz RBAC | **P1** | Sprint 3 (Próximo en Diseñar) | [Ver en P1](./01_prioridad_p1_alta.md#pb-039) |
| **PB-006** | `[CORE] [ARQUITECTURA]` | Desacoplamiento Multi-Motor SQL | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-006) |
| **PB-007** | `[POS] [NEGOCIO]` | Facturación Electrónica Oficial | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-007) |
| **PB-008** | `[POS] [INFRA]` | Impresión Física ESC/POS (Comanderas de Red) | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-008) |
| **PB-013** | `[CORE] [ARQUITECTURA]` | Evolución a Modelo de Dominio Rico | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-013) |
| **PB-014** | `[INVENTORY] [ARQUITECTURA]` | Strategy Pattern Dinámico en Kardex | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-014) |
| **PB-015** | `[POS] [ARQUITECTURA]` | Secuencia Diaria de Comandas Desacoplada | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-015) |
| **PB-036** | `[CATALOG] [UI/UX]` | Catálogo Visual con Imágenes de Platos en POS | **P2** | Sprint 5 (Planificado) | [Ver en P2](./02_prioridad_p2_media.md#pb-036) |
| **PB-040** | `[UI/UX] [NEGOCIO]` | Tour Guiado Interactivo (Product Tour Onboarding) | **P2** | Sprint 3 (Planificado) | [Ver en P2](./02_prioridad_p2_media.md#pb-040) |
| **PB-009** | `[SALON] [UI/UX]` | Mapa Visual Interactivo de Mesas | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-009) |
| **PB-010** | `[POS] [UI/UX]` | Panel KDS Táctil con Métricas de Despacho | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-010) |
| **PB-038** | `[SALON] [NEGOCIO]` | Gestión de Reservas y Capacidad de Salón | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-038) |
