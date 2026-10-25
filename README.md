# Sistema Integral de Gestión para Restaurante (POS + ERP)

Monolito modular de alto rendimiento para la gestión operativa (POS, Kardex/Recetas) y financiera (Arqueos, Gastos, Utilidad Real) de restaurantes, diseñado con arquitectura offline-first y estricto aislamiento multi-tenant.

---

## 🛠️ Stack Tecnológico

- **Backend:** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith, PostgreSQL 16, Flyway, Maven.
- **Frontend:** Angular 22 (Zoneless, Signal Forms, Standalone Components), Tailwind CSS, Dexie.js (IndexedDB / Offline-first).
- **Entorno de Desarrollo:** Windows 11 + WSL2 (Linux Fedora), Docker / Docker Compose.

---

## 📚 Estructura de Documentación

La documentación técnica y de negocio del proyecto se organiza en `docs/`:

```text
docs/
├── adr/                  # Decisiones Arquitectónicas (Architecture Decision Records)
│   ├── 0001_spring_modulith_monolith.md
│   ├── 0002_hybrid_tenancy_and_dynamic_settings.md
│   ├── 0003_offline_first_dexie_sync.md
│   ├── 0004_catalog_cloning_and_overrides.md
│   ├── 0005_cash_shift_expense_workflow.md
│   └── 0006_datetime_and_timezone_handling.md
├── sdd/                  # Especificación de Diseño del Sistema (System Design Document)
│   ├── 01_architecture_overview.md
│   ├── 02_backend_spec.md
│   ├── 03_frontend_spec.md
│   ├── 04_database_schema.md
│   └── 05_design_system.md
├── project/              # Documentos de Alcance, Gestión y Bóveda de Arquitectura
│   ├── architecture/     # 🏛️ Bóveda de Arquitectura Viva y Diagramas Mermaid (Obsidian Vault)
│   │   ├── README.md     # Índice raíz y grafo de navegación en Obsidian
│   │   ├── 01-c4-system/ # C4 Nivel 2 (Contenedores) y Nivel 3 (Backend Modulith y Frontend Angular)
│   │   ├── 02-domain-and-data/ # Modelo de Dominio, ERD PostgreSQL (18 tablas) y Esquema Dexie.js
│   │   ├── 03-use-cases/ # Casos de Uso Macro y por Módulo (Catálogo, POS, Kardex, Finanzas)
│   │   └── 04-behavior-and-flows/ # Secuencias POS/Kardex, Sync Idempotente, Estados y Strategy
│   ├── project_profile.md
│   ├── product_backlog.md
│   └── retrospectiva_negocio_sprint1.md
├── lessons_learned/      # Lecciones Aprendidas y Guías de Integración de Entorno
│   └── mcp-chrome-devtools-wsl2.md   # Integración Chrome DevTools MCP con WSL2 y Windows
└── tasks.md              # Plan de ejecución y seguimiento de tareas
```

### 🏛️ Bóveda de Arquitectura Viva en Obsidian (Specification-Driven Development)

El proyecto adopta **Specification-Driven Development (SDD)**, donde el código es la materialización de especificaciones técnicas y diagramas Mermaid vivos que evolucionan en estricta sincronía con el software:

- **Índice y Grafo Central:** Consulta [docs/project/architecture/README.md](docs/project/architecture/README.md) para abrir la bóveda en Obsidian.
- **Topología C4:** Modelado de contenedores y componentes en [01-c4-system/](docs/project/architecture/01-c4-system/).
- **Dominio y Base de Datos:** Entidades puras y ERD físico PostgreSQL en [02-domain-and-data/](docs/project/architecture/02-domain-and-data/).
- **Casos de Uso y RBAC:** Matriz de roles y capacidades operativas en [03-use-cases/](docs/project/architecture/03-use-cases/).
- **Flujos Transaccionales:** Secuencias de cobro con eventos asíncronos (`OrderPaidEvent`), sincronización offline-first con Dexie.js y máquinas de estado en [04-behavior-and-flows/](docs/project/architecture/04-behavior-and-flows/).
- **Gobernanza SDD:** Protocolos *Spec-First* y *Sync-Always* descritos en [.agents/skills/sdd-architecture-diagramming/SKILL.md](.agents/skills/sdd-architecture-diagramming/SKILL.md).


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
