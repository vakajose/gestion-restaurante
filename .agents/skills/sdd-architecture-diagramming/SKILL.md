---
name: sdd-architecture-diagramming
description: >-
  Estándar de Specification-Driven Development (SDD) y gobernanza de artefactos vivos de arquitectura.
  Usar para diseñar especificaciones/diagramas antes de implementar o para sincronizarlos obligatoriamente ante modificaciones en el código.
---

# Gobernanza de Specification-Driven Development (SDD) y Artefactos Vivos

Estándar metodológico para el desarrollo basado en especificaciones (**Specification-Driven Development - SDD**), donde los diagramas Mermaid en `docs/` actúan como artefactos vivos sincronizados con el código.

---

## 1. Jerarquía de Artefactos de Especificación

1. **Architecture Decision Records (`docs/adr/`):** Decisiones estructurales y sus trade-offs (`ADR-0001` a `ADR-0033`).
2. **System Design Documents (`docs/sdd/` y `docs/backoffice/sdd/`):** Especificaciones técnicas por capa para backend, ERP Angular, Backoffice React, base de datos y sistema de diseño.
3. **Bóveda de Arquitectura en Obsidian (`docs/project/architecture/` y `docs/backoffice/architecture/`):** Modelos C4, dominio, ERD (`56` tablas `V1`–`V24`), esquema local Dexie.js, casos de uso y flujos dinámicos en Mermaid.
4. **Código Fuente y Tests:** Backend Java 25 / Spring Modulith (`backend/`), ERP Angular 22 Zoneless + Dexie.js (`frontend/`), Backoffice React 19 (`backoffice-frontend/`).

> [!IMPORTANT]
> Los diagramas Mermaid son **artefactos vivos de especificación** que evolucionan en estricta sincronía con el código.

---

## 2. Ciclo de Vida SDD-SDLC (ADR-0007)

1. **Fase 1 (Inception / Análisis):** Requerimiento (`PB-xxx`) + co-diseño (`/grill-me`) $\rightarrow$ Caso de Uso (`UC-xxx`).
2. **Fase 2 (Elaboration / Spec-First):** Modelo de dominio, ERD, contratos `.api`, RFC 7807, diagramas Mermaid y ADR $\rightarrow$ Tareas en `docs/tasks.md`.
3. **Fase 3 (Construction):** Implementación en worktree aislado y rama `feat/...` con tests N1–N4.
4. **Fase 4 (Transition / Sync-Always):** Auditoría de convergencia código-diseño, sincronización documental (`doc-architect`) y cierre `[x]` en `docs/tasks.md`.

### Vía Estándar (Spec-First) vs Fast-Track
- **Vía A (Spec-First, por defecto):** Obligatoria para nuevas capacidades, nuevos módulos, cambios de esquema BD o contratos `.api`.
- **Vía B (Fast-Track, excepcional):** Solo para *hotfixes* críticos o ajustes cosméticos menores; sigue exigiendo Fase 4 (Sync-Always) antes de cerrar la tarea.

---

## 3. Taxonomía Real de la Bóveda Obsidian (`docs/`)

```text
docs/                                          # 🏛️ RAÍZ DE LA BÓVEDA OBSIDIAN
├── README.md                                  # 🗺️ Mapa Global de Contenidos (MOC)
├── .obsidian/                                 # Configuración Obsidian (enlaces relativos)
├── adr/                                       # ADRs (ADR-0001 a ADR-0033)
├── sdd/                                       # SDD ERP & Sistema (01 a 05)
├── backoffice/                                # 🛠️ Sub-Bóveda Backoffice SaaS (React + Control Plane)
│   ├── README.md
│   ├── sdd/                                   # 01 a 04 (Arquitectura, API, BD V17-V22, React 19)
│   └── architecture/                          # C4, Dominio/ERD, Use Cases, Secuencias/Estados
├── manuals/                                   # 📘 Manuales Operativos y Puesta en Marcha
│   ├── plataforma/                            # Bootstrap desde cero y Operación Backoffice SaaS
│   └── restaurante/                           # Manual operativo ERP/POS por rol y módulo (00..06)
├── business-processes/                        # 📖 Procesos de Negocio (01-pos a 04-admin)
├── testing/                                   # 🛡️ Bóveda QA, Inventario, UAT, E2E Guide e historico/
├── tasks.md                                   # 🚀 Backlog Activo (Sprint 6 / release/sprint-6)
├── observaciones_y_hallazgos.md               # Inbox de triage UAT
├── lessons_learned/                           # Lecciones aprendidas de entorno
└── project/                                   # Gestión y Bóveda de Arquitectura ERP
    ├── product_backlog/                       # Product Backlog modular (P0 a P3 + README)
    ├── sprints/                               # Bóveda de Sprints (1 a 6)
    ├── project_profile.md
    └── architecture/                          # 📐 Diagramas Vivos Mermaid
        ├── README.md
        ├── 01-c4-system/                      # c4_level2_containers, c4_level3_backend_modulith, c4_level3_frontend_angular
        ├── 02-domain-and-data/                # domain_model, database_erd, dexie_local_schema
        ├── 03-use-cases/                      # use_cases_overview + 8 módulos
        └── 04-behavior-and-flows/             # Flujos dinámicos (secuencias, estados y actividad)
```

---

## 4. Estándar de Formato Obsidian

1. **Frontmatter YAML Obligatorio:**
   ```yaml
   ---
   title: "Título Descriptivo del Artefacto"
   category: "architecture"
   tags:
     - architecture
     - mermaid
     - sdd
   last_updated: "2026-11-17"
   status: "verified"
   ---
   ```
2. **Sintaxis Mermaid Limpia:** Usar comillas dobles para etiquetas con paréntesis o símbolos (`id["Nodo (Detalle)"]`) y evitar HTML no estándar.
3. **Enlaces Portables Estándar:** Usar **exclusivamente** enlaces Markdown relativos (por ejemplo `[c4_level2_containers.md](../../../docs/project/architecture/01-c4-system/c4_level2_containers.md)`). Están **PROHIBIDOS** los enlaces wiki con dobles corchetes y las URIs absolutas locales.
