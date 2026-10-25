---
name: sdd-architecture-diagramming
description: >-
  Estándar de Specification-Driven Development (SDD) y gobernanza de artefactos vivos de arquitectura.
  Usar para diseñar especificaciones/diagramas antes de implementar o para sincronizarlos obligatoriamente ante modificaciones en el código.
---

# Gobernanza de Specification-Driven Development (SDD) y Artefactos Vivos

Este documento establece la metodología y los estándares de ingeniería para el desarrollo basado en especificaciones (**Specification-Driven Development - SDD**), donde los diagramas en Mermaid son artefactos visuales de primera clase que viven junto al código fuente y a la documentación técnica del proyecto.

---

## 1. Filosofía de Specification-Driven Development (SDD)

En este proyecto, el código no es un ente aislado; es la materialización ejecutable de una especificación formal. Los artefactos de especificación se complementan jerárquicamente:
1. **Architecture Decision Records (ADR):** Justificación del "por qué" de las decisiones estructurales y tecnológicas (`docs/adr/`).
2. **System Design Documents (SDD):** Especificación técnica formal capa a capa (`docs/sdd/`).
3. **Bóveda de Arquitectura en Obsidian con Diagramas Mermaid:** Artefactos visuales dinámicos que modelan la topología C4, el dominio, el modelo físico relacional/local y los flujos transaccionales (`docs/project/architecture/`).
4. **Código Fuente y Tests:** Implementación en Java 25 / Spring Modulith y Angular 22 Zoneless con Dexie.js.

> [!IMPORTANT]
> Los diagramas Mermaid **no son documentación estática descartable**, sino **artefactos vivos de especificación** que evolucionan en estricta sincronía con el software.

---

## 2. Los Dos Flujos de Trabajo en SDD

### Flujo A: Spec-First / Forward (Diseño y Análisis Previo)
Recomendado y preferente para nuevas capacidades, adición de módulos, o refactors estructurales complejos:
1. **Análisis y Modelado:** Redactar o actualizar la especificación en `docs/sdd/` y diseñar los diagramas Mermaid correspondientes en `docs/project/architecture/` (Casos de Uso, Modelo de Datos ERD, Secuencia o Estados).
2. **Revisión y Aprobación:** Validar los diagramas y contratos con el equipo/usuario.
3. **Generación de Tareas:** Derivar las tareas atómicas de implementación en `docs/tasks.md` directamente a partir de la especificación aprobada.
4. **Implementación y Verificación:** Desarrollar el código backend/frontend y verificar con pruebas automatizadas (`./mvnw clean test`, `npm test`).

### Flujo B: Sync-Always / Backward (Sincronización Obligatoria en Código)
**Regla Inviolable:** Cuando una tarea implique modificar el comportamiento, la estructura de datos o los contratos de un componente existente:
1. **Detección de Impacto:** Identificar qué artefactos de especificación se ven afectados:
   - ¿Cambió un contrato `.api` o endpoint REST? $\rightarrow$ Actualizar diagramas C4 Componentes y Secuencia.
   - ¿Cambió una tabla PostgreSQL o almacenamiento Dexie? $\rightarrow$ Actualizar `database_erd.md` o `dexie_local_schema.md`.
   - ¿Cambió una transición de estado o regla de negocio? $\rightarrow$ Actualizar diagramas de Estado (`stateDiagram-v2`) o Actividad.
2. **Sincronización Obligatoria:** Actualizar los archivos `.md` y los bloques ````mermaid ```` correspondientes dentro de la misma tarea y commit.
3. **Criterio de Aceptación (Definition of Done):** Ninguna tarea se marca como `[x]` en `docs/tasks.md` si su código difiere de su especificación o si sus diagramas no reflejan la realidad del sistema.

---

## 3. Taxonomía de la Bóveda Obsidian (`docs/`)

La totalidad de `docs/` es la bóveda de Obsidian. Los diagramas y modelos arquitectónicos se concentran en `docs/project/architecture/`, mientras que el resto de especificaciones y decisiones se integran en la misma bóveda:

```text
docs/                                          # 🏛️ RAÍZ DE LA BÓVEDA OBSIDIAN
├── README.md                                  # 🗺️ Mapa Global de Contenidos (MOC)
├── .obsidian/                                 # Configuración Obsidian (enlaces relativos)
├── adr/                                       # Architecture Decision Records (ADR-0001 a ADR-0006)
├── sdd/                                       # System Design Documents (01 a 05)
├── tasks.md                                   # Backlog de Tareas y Seguimiento
├── lessons_learned/                           # Lecciones Aprendidas de Entorno
└── project/                                   # Negocio, Backlog y Bóveda de Arquitectura
    ├── project_profile.md
    ├── product_backlog.md
    ├── retrospectiva_negocio_sprint1.md
    └── architecture/                          # 📐 Bóveda de Arquitectura y Diagramas Mermaid
        ├── README.md                          # Sub-índice especializado de Arquitectura
        ├── 01-c4-system/                      # C4 Model: Contenedores y Componentes
        │   ├── c4_level2_containers.md
        │   ├── c4_level3_backend_modulith.md
        │   └── c4_level3_frontend_angular.md
        ├── 02-domain-and-data/                # Dominio, ERD PostgreSQL y Dexie.js
        │   ├── domain_model.md
        │   ├── database_erd.md
        │   └── dexie_local_schema.md
        ├── 03-use-cases/                      # Mapa de Actores y Casos de Uso por módulo
        │   ├── use_cases_overview.md
        │   ├── use_cases_catalog.md
        │   ├── use_cases_pos.md
        │   ├── use_cases_inventory.md
        │   └── use_cases_finance.md
        └── 04-behavior-and-flows/             # Secuencias complejas, Estados y Actividades
            ├── sequence_pos_order_deduction.md
            ├── sequence_pos_offline_sync.md
            ├── state_order_lifecycle.md
            ├── state_shift_and_expense_workflow.md
            └── activity_inventory_deduction_strategy.md
```


---

## 4. Estándar de Formato para Compatibilidad con Obsidian

1. **Frontmatter YAML Obligatorio:**
   ```yaml
   ---
   title: "Título Descriptivo del Artefacto"
   category: "architecture"
   level: "C4-Container | Domain | ERD | Use-Case | Sequence | State | Activity"
   module: "global | core | catalog | pos | inventory | finance"
   tags:
     - architecture
     - mermaid
     - sdd
   last_updated: "YYYY-MM-DD"
   status: "verified"
   ---
   ```
2. **Sintaxis Mermaid Limpia:**
   - Usar comillas dobles para etiquetas con paréntesis o caracteres especiales: `id["Nodo (Detalle)"]`.
   - Evitar HTML embebido no estándar dentro de nodos.
   - Declarar relaciones explícitas y consistentes con la terminología del código.
3. **Enlaces Portables:** Usar siempre rutas relativas en formato Markdown estándar `[Texto](../carpeta/archivo.md)` para que funcionen idénticamente en GitHub, IDEs y Obsidian.
