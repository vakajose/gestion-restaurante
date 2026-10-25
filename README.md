# Sistema Integral de Gestión para Restaurante (POS + ERP)

Monolito modular de alto rendimiento para la gestión operativa (POS, Kardex/Recetas) y financiera (Arqueos, Gastos, Utilidad Real) de restaurantes, diseñado con arquitectura offline-first y estricto aislamiento multi-tenant.

---

## 🛠️ Stack Tecnológico

- **Backend:** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith, PostgreSQL 16, Flyway, Maven.
- **Frontend:** Angular 22 (Zoneless, Signal Forms, Standalone Components), Tailwind CSS, Dexie.js (IndexedDB / Offline-first).
- **Entorno de Desarrollo:** Windows 11 + WSL2 (Linux Fedora), Docker / Docker Compose.

---

## 📚 Estructura de Documentación y Bóveda Obsidian (`docs/`)

La totalidad de la carpeta `docs/` conforma la **Bóveda Unificada de Obsidian** del proyecto. Diseñada bajo el estándar de **Specification-Driven Development (SDD)**, organiza el conocimiento, decisiones y arquitectura viva con renderizado Mermaid nativo y metadatos YAML compatibles con Dataview:

```text
docs/                             # 🏛️ BÓVEDA OBSIDIAN (Abrir esta carpeta como Vault)
├── README.md                     # 🗺️ Índice Raíz y Mapa Global de Contenidos (MOC)
├── .obsidian/                    # Configuración de bóveda (enlaces relativos Markdown estándar)
├── adr/                          # Decisiones Arquitectónicas (Architecture Decision Records)
│   ├── 0001_spring_modulith_monolith.md
│   ├── 0002_hybrid_tenancy_and_dynamic_settings.md
│   ├── 0003_offline_first_dexie_sync.md
│   ├── 0004_catalog_cloning_and_overrides.md
│   ├── 0005_cash_shift_expense_workflow.md
│   └── 0006_datetime_and_timezone_handling.md
├── sdd/                          # Especificación de Diseño del Sistema (System Design Document)
│   ├── 01_architecture_overview.md
│   ├── 02_backend_spec.md
│   ├── 03_frontend_spec.md
│   ├── 04_database_schema.md
│   └── 05_design_system.md
├── project/                      # Alcance de Negocio, Backlog y Bóveda de Arquitectura
│   ├── architecture/             # 📐 Diagramas Vivos Mermaid y Especificaciones Dinámicas
│   │   ├── README.md             # Sub-índice especializado de Arquitectura
│   │   ├── 01-c4-system/         # C4 Nivel 2 (Contenedores) y Nivel 3 (Backend Modulith y Frontend Angular)
│   │   ├── 02-domain-and-data/   # Modelo de Dominio, ERD PostgreSQL (19 tablas) y Esquema Dexie.js
│   │   ├── 03-use-cases/         # Casos de Uso Macro y por Módulo (Catálogo, POS, Kardex, Finanzas)
│   │   └── 04-behavior-and-flows/# Secuencias POS/Kardex, Sync Idempotente, Estados y Strategy
│   ├── project_profile.md
│   ├── product_backlog.md
│   └── retrospectiva_negocio_sprint1.md
├── lessons_learned/              # Lecciones Aprendidas y Guías de Integración de Entorno
│   └── mcp-chrome-devtools-wsl2.md # Integración Chrome DevTools MCP con WSL2 y Windows
└── tasks.md                      # Plan de ejecución y seguimiento atómico de tareas
```

### 🏛️ Cómo Usar la Bóveda en Obsidian

1. **Abrir Bóveda:** En Obsidian, selecciona **"Open folder as vault"** y apunta a la carpeta `docs/`.
2. **Índice y Grafo Central (MOC):** Inicia la navegación desde [docs/README.md](docs/README.md).
3. **Exploración por Grafo:** Presiona `Ctrl + G` para visualizar el grafo interconectado entre requerimientos de negocio, ADRs, especificaciones SDD y diagramas Mermaid.
4. **Gobernanza SDD (Spec-First y Sync-Always):** Consulta [.agents/skills/sdd-architecture-diagramming/SKILL.md](.agents/skills/sdd-architecture-diagramming/SKILL.md) para el protocolo obligatorio de diseño previo y sincronización continua de especificaciones.



---

## 🚀 Puesta en Marcha Local

### 1. Requisitos Previos
- Docker Desktop o Docker Engine activo.
- Java 25 LTS instalado (o via Maven Wrapper).
- Node.js (v20+) y npm.

### 2. Base de Datos
Iniciar el contenedor PostgreSQL:
```bash
docker compose -f docker/docker-compose.yml up -d
```

### 3. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
Disponible en: `http://localhost:8080` (las credenciales y tenants demo se inicializan automáticamente).

### 4. Frontend (Angular)
```bash
cd frontend
npm install
npm start
```
Disponible en: `http://localhost:4200`

---

## 🤖 Integración de Agentes y MCP (Model Context Protocol)

El proyecto cuenta con soporte para agentes de IA (Google Antigravity, Claude Code, Codex, Cursor):
- **Protocolo de Operación:** Consulta [AGENTS.md](AGENTS.md).
- **Herramientas MCP:** Consulta [.mcp/README.md](.mcp/README.md) y la plantilla [.mcp/config.example.json](.mcp/config.example.json).
- **Automatización de Navegador e2e:** Guía completa en [mcp-chrome-devtools-wsl2.md](docs/lessons_learned/mcp-chrome-devtools-wsl2.md).
