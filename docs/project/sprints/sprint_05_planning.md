---
title: "Sprint 5 Planning: Servicio Avanzado y Candidato a Producción"
category: "sprints"
sprint: 5
status: "archived"
tags:
  - sprints
  - planning
  - sprint-5
  - archived
last_updated: "2026-11-17"
---

# 📅 Sprint 5 Planning — Servicio Avanzado y Candidato a Producción (`v0.5.0` — Archivado)

> **Estado del Sprint:** `ARCHIVADO / COMPLETADO EN SPRINT 5` — Ver ejecución certificada en [`sprint_05_backlog.md`](./sprint_05_backlog.md)  
> **Objetivo del Sprint:** Consolidar la experiencia integral de servicio gastronómico de cara al cliente: modificadores y extras con/sin costo y deducción Kardex (`PB-020`), workflow de cancelaciones y devoluciones con mermas (`PB-031`), pedidos para llevar y delivery (`PB-032`), catálogo visual con fotos en POS (`PB-036`), auditoría gerencial de turnos históricos (`PB-030`), perfil de usuario autoservicio (`PB-045`), humanización de parametría (`PB-059`, antes `PB-047`) y asistente de codificación de catálogo (`PB-048`).

---

## 📋 Tareas Planificadas del Sprint 5

### 1. Modificadores, Complementos y Personalización de Comandas (`PB-020`)
- [x] **TASK-501 [POS] [NEGOCIO]:** Suite de Modificadores, Complementos y Extras con Configuración de Costo y Kardex (`PB-020`).
- [x] **TASK-502 [POS] [UI/UX]:** Selector Interactivo de Modificadores en POS y Notas de Cocina en Comanda (`PB-020`).

### 2. Cancelaciones, Devoluciones y Tratamiento de Mermas (`PB-031`)
- [x] **TASK-503 [POS] [DISEÑO]:** Máquina de Estados para Cancelaciones, Autorización con PIN y Reversión a Kardex vs Merma (`PB-031`).
- [x] **TASK-504 [POS] [NEGOCIO]:** Implementación de Reintegro de Efectivo en Gaveta y Registro de Merma por Cancelación (`PB-031`).

### 3. Modalidades Para Llevar (`Takeout`) y `Delivery` (`PB-032`)
- [x] **TASK-505 [POS] [NEGOCIO]:** Modalidades de Servicio (`DINE_IN`, `TAKEOUT`, `DELIVERY`) y Distintivos en Tickets (`PB-032`).

### 4. Catálogo Visual con Imágenes y Codificación Secuencial (`PB-036` / `PB-048`)
- [x] **TASK-506 [CATALOG] [UI/UX]:** Visualización de Imágenes de Platos en Cuadrícula POS con Toggle Parametrizable (`PB-036`).
- [x] **TASK-507 [CATALOG] [UI/UX]:** Asistente de SKU Secuencial (`BEB-1`) y Selector de Emojis para Categorías (`PB-048`).

### 5. Auditoría Gerencial y Autoservicio de Perfil (`PB-030` / `PB-045` / `PB-059`)
- [x] **TASK-508 [FINANCE] [BI]:** Histórico Gerencial de Turnos con Filtros por Fechas, Sucursal y Exportación (`PB-030`).
- [x] **TASK-509 [AUTH] [UI/UX]:** Perfil de Usuario Autoservicio, Despliegue de Nombres Reales y Limpieza de Privilegios (`PB-045`).
- [x] **TASK-510 [CONFIG] [UI/UX]:** Humanización de Tarjetas de Parámetros y Opciones en Español (`PB-059`, antes `PB-047`).

### 6. Certificación Final Pre-Producción
- [x] **TASK-511 [QA] [CALIDAD]:** Certificación E2E Integral de Todos los Flujos de Negocio de Restaurante (`DoD Producción`).

> [!NOTE]
> **Reasignación de Inicialización Limpia de Sistema (`PB-057`):**
> La tarea originalmente numerada como `TASK-512` (Scripts de Inicialización de Sistema Limpio para Go-Live / *Baseline System Startup*) fue reasignada oficialmente al **Sprint 6** como **`TASK-602`**, integrándose en el bootstrap limpio de plataforma y desacoplamiento `SUPERADMIN`.
