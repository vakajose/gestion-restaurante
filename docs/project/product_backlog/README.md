---
title: "Product Backlog y Registro de Nuevas Funcionalidades"
category: "product-backlog"
tags:
  - backlog
  - roadmap
  - requirements
  - priorities
  - sdd
last_updated: "2026-11-19"
status: "active"
---

# 📋 Product Backlog y Registro de Nuevas Funcionalidades

Este documento constituye el **Índice Central y Matriz de Priorización** del Product Backlog del Sistema de Gestión de Restaurante (`POS + ERP + Backoffice SaaS`).

Para optimizar la consulta por parte de desarrolladores y agentes de IA y evitar sobrecarga de contexto, el backlog se encuentra modularizado por nivel de criticidad en archivos independientes (`PB-001` a `PB-067` sin colisiones):

| Nivel de Prioridad | Enfoque Operativo | Archivo de Especificación |
| :--- | :--- | :--- |
| 🔴 **P0 (Máxima Prioridad / Crítica)** | Aislamiento Multi-Tenant, Onboarding, RBAC, Backoffice SaaS, Parametría Core y Estabilidad CI | [**`00_prioridad_p0_critica.md`**](./00_prioridad_p0_critica.md) |
| 🟠 **P1 (Alta Prioridad)** | Operación POS, Finanzas, Kardex, Sesiones, Roles, Storage, Estabilización MVP y Versión Futura | [**`01_prioridad_p1_alta.md`**](./01_prioridad_p1_alta.md) |
| 🟡 **P2 (Media Prioridad)** | Hardware ESC/POS, Facturación, Catálogo Visual, Humanización UI y Unificación Conceptual | [**`02_prioridad_p2_media.md`**](./02_prioridad_p2_media.md) |
| 🟢 **P3 (Baja / Futuro)** | Salón Interactivo, KDS Cocina, Reservas de Mesas y Gobernanza Avanzada | [**`03_prioridad_p3_baja_futuro.md`**](./03_prioridad_p3_baja_futuro.md) |

---

## 🧭 Matriz de Priorización de Negocio

El backlog se gobierna mediante la siguiente escala:
1. **P0 (Máxima Prioridad):** Requerimientos indispensables para la arquitectura multi-inquilino, seguridad fundacional, backoffice de plataforma y ciclo de vida de la empresa.
2. **P1 (Alta Prioridad):** Funcionalidades requeridas para la operación diaria fluida del restaurante, despliegue cloud y madurez del MVP.
3. **P2 (Media Prioridad):** Integraciones de hardware, optimizaciones de diseño, refactorizaciones estructurales no bloqueantes y ampliaciones de catálogo.
4. **P3 (Baja Prioridad / Futuro):** Extensiones para etapas de escalamiento comercial maduro.

---

## 📑 Tabla Maestra de Requerimientos y Estado (`PB-001`–`PB-067`)

| ID | Módulo / Tipo | Título del Requerimiento | Prioridad | Estado / Sprint | Archivo |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **PB-001** | `[GLOBAL] [NEGOCIO]` | Onboarding del Cliente y Alta Rápida de Empresa | **P0** | Completado (Sprint 3) | [Ver en P0](./00_prioridad_p0_critica.md#pb-001) |
| **PB-002** | `[GLOBAL] [SEGURIDAD]` | Rol "Árbitro del Sistema" (`SUPERADMIN` SaaS) | **P0** | Completado (Sprint 6) | [Ver en P0](./00_prioridad_p0_critica.md#pb-002) |
| **PB-003** | `[GLOBAL] [ARQUITECTURA]` | Backoffice Independiente (SPA React LTS + Módulo Spring) | **P0** | Completado (Sprint 6) | [Ver en P0](./00_prioridad_p0_critica.md#pb-003) |
| **PB-017** | `[COMPANY] [NEGOCIO]` | Gestión de Empresa (Tenant) y Red de Sucursales | **P0** | Completado (Sprint 2) | [Ver en P0](./00_prioridad_p0_critica.md#pb-017) |
| **PB-018** | `[AUTH] [SEGURIDAD]` | RBAC y Gestión de Usuarios por Tenant | **P0** | Completado (Sprint 2) | [Ver en P0](./00_prioridad_p0_critica.md#pb-018) |
| **PB-019** | `[AUTH] [SEGURIDAD]` | Autenticación por Tenant Identifier y Selector Sucursal | **P0** | Completado (Sprint 2) | [Ver en P0](./00_prioridad_p0_critica.md#pb-019) |
| **PB-022** | `[DEVOPS] [CALIDAD]` | Corrección de Suite de Pruebas Frontend en CI | **P0** | Completado (`TASK-011`) | [Ver en P0](./00_prioridad_p0_critica.md#pb-022) |
| **PB-023** | `[DOCS] [ARQUITECTURA]` | Bóveda de Arquitectura Viva en Mermaid (SDD) | **P0** | Completado (`TASK-012`..`017`) | [Ver en P0](./00_prioridad_p0_critica.md#pb-023) |
| **PB-026** | `[CONFIG] [ARQUITECTURA]` | Parametrización Multinivel con Hot-Reload | **P0** | Completado (Sprint 3) | [Ver en P0](./00_prioridad_p0_critica.md#pb-026) |
| **PB-041** | `[POS] [SEGURIDAD/NEGOCIO]` | Invariante de Venta en Turno Abierto y Blindaje POS | **P0** | Completado (Sprint 4) | [Ver en P0](./00_prioridad_p0_critica.md#pb-041) |
| **PB-004** | `[INVENTORY] [NEGOCIO]` | Transferencias de Insumos entre Sucursales | **P1** | En Backlog | [Ver en P1](./01_prioridad_p1_alta.md#pb-004) |
| **PB-005** | `[FINANCE] [BI]` | Consolidación Financiera Multi-Sucursal | **P1** | En Backlog | [Ver en P1](./01_prioridad_p1_alta.md#pb-005) |
| **PB-011** | `[GLOBAL] [ARQUITECTURA]` | Desacoplamiento de Repositorios a APIs Públicas | **P1** | Completado (Sprint 2) | [Ver en P1](./01_prioridad_p1_alta.md#pb-011) |
| **PB-012** | `[GLOBAL] [ARQUITECTURA]` | Excepciones de Dominio y ProblemDetail RFC 7807 | **P1** | Completado (Sprint 2) | [Ver en P1](./01_prioridad_p1_alta.md#pb-012) |
| **PB-016** | `[POS] [ARQUITECTURA]` | Repositorios Locales DAL para Dexie.js | **P1** | Completado (Sprint 2) | [Ver en P1](./01_prioridad_p1_alta.md#pb-016) |
| **PB-020** | `[POS] [NEGOCIO]` | Motor de Promociones, Combos, Modificadores y Notas | **P1** | Completado (Sprint 5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-020) |
| **PB-021** | `[INVENTORY] [NEGOCIO]` | Inicialización de Stock, Compras, Lotes y Mermas | **P1** | Parcial (`TASK-206`, `635` / `636` Post-MVP) | [Ver en P1](./01_prioridad_p1_alta.md#pb-021) |
| **PB-024** | `[FINANCE] [BI]` | Panel de Analítica y Rentabilidad Neta | **P1** | Pospuesto | [Ver en P1](./01_prioridad_p1_alta.md#pb-024) |
| **PB-025** | `[DEVOPS] [INFRA]` | Despliegue, Contenerización Docker y Cloud OCI | **P1** | Completado (`TASK-601/606/608/609/610`) | [Ver en P1](./01_prioridad_p1_alta.md#pb-025) |
| **PB-027** | `[AUTH] [SEGURIDAD]` | Ciclo de Vida de Sesiones (JWT Refresh & Timeout) | **P1** | Completado (Sprint 3) | [Ver en P1](./01_prioridad_p1_alta.md#pb-027) |
| **PB-028** | `[UI/UX] [ARQUITECTURA]` | Diseño Responsivo, Sidebar Colapsable y Ergonomía | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-028) |
| **PB-029** | `[FINANCE] [NEGOCIO]` | Visor de Ventas del Turno Activo para Cajero | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-029) |
| **PB-030** | `[FINANCE] [BI]` | Histórico Gerencial de Turnos y Auditoría de Ventas | **P1** | Completado (Sprint 5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-030) |
| **PB-031** | `[POS] [NEGOCIO]` | Cancelación de Pedidos, Devolución y Kardex | **P1** | Completado (Sprint 5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-031) |
| **PB-032** | `[POS] [NEGOCIO]` | Pedidos "Para Llevar" (Takeout) y Delivery | **P1** | Completado (Sprint 5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-032) |
| **PB-033** | `[INVENTORY] [ARQUITECTURA]` | Modelo de Inventario Centralizado vs Sucursal | **P1** | En Backlog | [Ver en P1](./01_prioridad_p1_alta.md#pb-033) |
| **PB-034** | `[CATALOG/POS] [NEGOCIO]` | Menús Diarios, Plantillas y Selección en Turno | **P1** | Completado (Sprint 4 / Amp. S6) | [Ver en P1](./01_prioridad_p1_alta.md#pb-034) |
| **PB-035** | `[STORAGE] [ARQUITECTURA]` | Almacenamiento Desacoplado Agnóstico (Local / S3 / R2 / OCI) | **P1** | Completado (Sprint 6) | [Ver en P1](./01_prioridad_p1_alta.md#pb-035) |
| **PB-037** | `[CATALOG] [NEGOCIO]` | Exclusión de Insumos BOM y Notas de Cocina | **P1** | Completado (Sprint 5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-037) |
| **PB-039** | `[AUTH] [SEGURIDAD]` | Roles Personalizados, Privilegios y Matriz RBAC | **P1** | Completado (Sprint 3) | [Ver en P1](./01_prioridad_p1_alta.md#pb-039) |
| **PB-042** | `[UI/UX] [ARQUITECTURA]` | Design System de Restaurante y Suite Componentes | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-042) |
| **PB-043** | `[UI/UX] [ARQUITECTURA]` | Motor Unificado de Notificaciones (Toasts Z-Index) | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-043) |
| **PB-044** | `[CATALOG] [NEGOCIO]` | Modelo Flexible de Recetas (BOM vs Unidad vs Libre) | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-044) |
| **PB-046** | `[QA] [CALIDAD]` | Batería E2E Crítica de Negocio y Ciclo Continuo | **P1** | Completado (Sprint 4/5) | [Ver en P1](./01_prioridad_p1_alta.md#pb-046) |
| **PB-047** | `[DEVOPS/DB] [CALIDAD]` | Scripts de Reset, Semillas y Aprovisionamiento Dev | **P1** | Completado (Sprint 4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-047) |
| **PB-049** | `[ONBOARDING] [UX]` | Rediseño Integral de Cero del Tutorial y Product Tour | **P1** | En Backlog (Retrospectiva S4) | [Ver en P1](./01_prioridad_p1_alta.md#pb-049) |
| **PB-057** | `[DEVOPS/STARTUP] [ARQUITECTURA]` | Arranque Limpio Go-Live (`Baseline Startup` sin Demo) | **P1** | Completado (Sprint 6) | [Ver en P1](./01_prioridad_p1_alta.md#pb-057) |
| **PB-058** | `[DEVSECOPS] [SEGURIDAD]` | Auditoría Local DevSecOps (Secretos, SAST/SCA y Docker) | **P1** | Completado (Sprint 6) | [Ver en P1](./01_prioridad_p1_alta.md#pb-058) |
| **PB-060** | `[CATALOG/POS] [MVP]` | Estabilización MVP v1: Catálogo Sucursal, Sesiones Compartidas, Combos Dinámicos e Insumos | **P1** | Completado (Sprint 6) | [Ver en P1](./01_prioridad_p1_alta.md#pb-060) |
| **PB-062** | `[DOCS/UX] [ERP/BO]` | Wiki / Manual Web Integrado en la Aplicación para Usuario Final | **P1** | En Backlog (`TASK-639`) | [Ver en P1](./01_prioridad_p1_alta.md#pb-062) |
| **PB-063** | `[CONFIG/POS/DEXIE] [ARQ]` | Completar Cableado de Parámetros, Selector `InventoryStrategy` y Caché Offline Combos/Menús | **P1** | En Backlog (`TASK-640`) | [Ver en P1](./01_prioridad_p1_alta.md#pb-063) |
| **PB-006** | `[CORE] [ARQUITECTURA]` | Desacoplamiento Multi-Motor SQL | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-006) |
| **PB-007** | `[POS] [NEGOCIO]` | Facturación Electrónica Oficial | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-007) |
| **PB-008** | `[POS] [INFRA]` | Impresión Física ESC/POS (Comanderas de Red) | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-008) |
| **PB-013** | `[CORE] [ARQUITECTURA]` | Evolución a Modelo de Dominio Rico | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-013) |
| **PB-014** | `[INVENTORY] [ARQUITECTURA]` | Strategy Pattern Dinámico en Kardex | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-014) |
| **PB-015** | `[POS] [ARQUITECTURA]` | Secuencia Diaria de Comandas Desacoplada | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-015) |
| **PB-036** | `[CATALOG] [UI/UX]` | Catálogo Visual con Imágenes de Platos en POS | **P2** | Completado (Sprint 5) | [Ver en P2](./02_prioridad_p2_media.md#pb-036) |
| **PB-040** | `[UI/UX] [NEGOCIO]` | Tour Guiado Interactivo (`Product Tour Onboarding`) | **P2** | Parcial (Sprint 3 — Rediseño `PB-049`) | [Ver en P2](./02_prioridad_p2_media.md#pb-040) |
| **PB-045** | `[AUTH] [UI/UX]` | Perfil de Usuario Autoservicio y Nombres Reales | **P2** | Completado (Sprint 5) | [Ver en P2](./02_prioridad_p2_media.md#pb-045) |
| **PB-048** | `[CATALOG] [UI/UX]` | Asistente de Códigos Secuenciales y Emojis | **P2** | Completado (Sprint 5) | [Ver en P2](./02_prioridad_p2_media.md#pb-048) |
| **PB-050** | `[CATALOG/POS] [UI/UX]` | Edición Dinámica y Duplicación de Menús en Turno | **P2** | En Backlog (Retrospectiva S4) | [Ver en P2](./02_prioridad_p2_media.md#pb-050) |
| **PB-059** | `[CONFIG] [UI/UX]` | Humanización y Etiquetas de Consola Parámetros *(antes PB-047 P2)* | **P2** | Completado (Sprint 5) | [Ver en P2](./02_prioridad_p2_media.md#pb-059) |
| **PB-061** | `[CATALOG/POS] [ARQ]` | Unificación de Elementos de Carta, Clasificadores Categoría (`0..*`) y `FULL_CATALOG` | **P2** | En Backlog (`TASK-637`, `638`) | [Ver en P2](./02_prioridad_p2_media.md#pb-061) |
| **PB-064** | `[INFRA/DOMINIOS] [ARQ]` | Separación de Dominios (`app.` vs raíz) y Aislamiento de Reputación Email | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-064) |
| **PB-065** | `[WEB/SEO] [MARKETING]` | SEO Técnico (`sitemap.xml`, `robots.txt`, Search Console) e Imagen `og:image` (`1200×630`) | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-065) |
| **PB-066** | `[ANALYTICS/UX] [BI]` | Analítica de Producto para Embudo de Onboarding y Detección de Fricción | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-066) |
| **PB-067** | `[PRIVACY/COMPLIANCE] [SEG]` | Gobernanza de Cookies, Banner Simétrico (Aceptar/Rechazar) y Carga Diferida | **P2** | En Backlog | [Ver en P2](./02_prioridad_p2_media.md#pb-067) |
| **PB-009** | `[SALON] [UI/UX]` | Mapa Visual Interactivo de Mesas | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-009) |
| **PB-010** | `[POS] [UI/UX]` | Panel KDS Táctil con Métricas de Despacho | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-010) |
| **PB-038** | `[SALON] [NEGOCIO]` | Gestión de Reservas y Capacidad de Salón | **P3** | En Backlog | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-038) |
| **PB-051** | `[ONBOARDING] [GIS]` | Proveedor de Mapas para Ubicación de Sucursal | **P3** | En Backlog (Retrospectiva S4) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-051) |
| **PB-052** | `[THEME] [UI/UX]` | Auditoría de Contraste y Normalización de Temas | **P3** | En Backlog (Retrospectiva S4) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-052) |
| **PB-053** | `[CORE] [ARQUITECTURA]` | Extracción Transversal del Motor de Autorizaciones | **P3** | En Backlog (Co-Diseño S5) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-053) |
| **PB-054** | `[POS] [OPERACIONES]` | Módulo Avanzado de Encargos Programados (`Scheduled Pickups`) | **P3** | En Backlog (Co-Diseño S5) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-054) |
| **PB-055** | `[CONFIG/FINANCE] [NEGOCIO]` | Parametrización de Métodos de Pago Habilitados por Sucursal | **P3** | En Backlog (Co-Diseño S5) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-055) |
| **PB-056** | `[CORE] [ARQUITECTURA/DATOS]` | Gobernanza de Eliminación Lógica (`Soft Deletes`) y Trazabilidad | **P3** | En Backlog (Co-Diseño S5) | [Ver en P3](./03_prioridad_p3_baja_futuro.md#pb-056) |
