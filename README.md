# Sistema Integral de Gestión para Restaurante (POS + ERP + Backoffice SaaS)

Plataforma modular multi-tenant para la gestión operativa (POS, Kardex/Recetas, Comandas, Delivery/Pickup) y financiera (Arqueos, Gastos, Utilidad Real) de restaurantes con arquitectura **offline-first**, acompañada de un **Backoffice SaaS independiente** para administración de plataforma, planes, suscripciones, cuotas y observabilidad.

---

## 🛠️ Stack Tecnológico

- **Backend (`backend/`):** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith, PostgreSQL 16, Flyway, Maven.
- **ERP Restaurante (`frontend/`):** Angular 22 (Zoneless, Signal Forms, Standalone Components), Tailwind CSS, Dexie.js (IndexedDB / Offline-first).
- **Backoffice SaaS (`backoffice-frontend/`):** React (LTS), TypeScript, Vite, Tailwind CSS.
- **Infraestructura & Observabilidad (`docker/`):** Docker Multi-Stage, Caddy (Reverse Proxy / TLS), Dozzle (Visor de logs), OCI Object Storage / S3-compatible.

---

## 📂 Estructura del Repositorio

```text
gestion-restaurante/
├── backend/                 # API Spring Boot + Spring Modulith (ERP + Backoffice)
├── frontend/                # SPA ERP Restaurante (Angular 22 + Dexie.js)
├── backoffice-frontend/     # SPA Backoffice SaaS (React LTS + Tailwind CSS)
├── docker/                  # Compose (dev, testing-local, staging, prod), Caddy y VM2 Postgres
├── scripts/                 # Scripts de respaldo/restauración S3, seguridad DevSecOps y ops
└── docs/                    # 🏛️ Bóveda Obsidian (ADRs, SDD, Sprints, Manuales y Pruebas)
```

---

## 📚 Documentación y Bóveda Obsidian (`docs/`)

La carpeta `docs/` conforma la **Bóveda Unificada de Obsidian** bajo el estándar **Specification-Driven Development (SDD)**:

- **Índice Central (MOC):** [docs/README.md](docs/README.md)
- **Manual de Bootstrap y Operación Inicial:** [docs/manuals/bootstrap_y_operacion_inicial.md](docs/manuals/bootstrap_y_operacion_inicial.md) *(cómo levantar desde cero, `.env` y primeras configuraciones en Backoffice)*
- **Infraestructura Docker y 4 Ambientes:** [docker/README.md](docker/README.md)
- **Sub-Bóveda Backoffice SaaS:** [docs/backoffice/README.md](docs/backoffice/README.md)
- **Backlog Activo:** [docs/tasks.md](docs/tasks.md) | **Sprints Históricos:** [docs/project/sprints/README.md](docs/project/sprints/README.md)
- **Gobernanza SDD-SDLC:** [ADR-0007](docs/adr/0007_sdd_software_development_lifecycle.md) y [.agents/skills/sdd-architecture-diagramming/SKILL.md](.agents/skills/sdd-architecture-diagramming/SKILL.md)

---

## 🚀 Puesta en Marcha

### Opción A: Entorno Completo Dockerizado (`testing-local`)
Emula la topología completa (`postgres`, `backend`, `erp-frontend`, `backoffice-frontend`, `dozzle`, `caddy`) sin seeds de demostración. Ver guía completa en [docs/manuals/bootstrap_y_operacion_inicial.md](docs/manuals/bootstrap_y_operacion_inicial.md).

```bash
docker compose -f docker/docker-compose.testing-local.yml up --build -d
```
- **ERP Restaurante:** `http://app.localhost`
- **Backoffice SaaS:** `http://admin.localhost` (`superadmin` / `SuperAdminLocal123!`)
- **API Health:** `http://api.localhost/actuator/health`
- **Logs (Dozzle):** `http://logs.localhost`

### Opción B: Desarrollo Diario Nativo (`dev`)
1. **Base de datos PostgreSQL:**
   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```
2. **Backend (Spring Boot con perfil `dev` y datos demo):**
   ```bash
   cd backend && ./mvnw spring-boot:run
   ```
   Disponible en `http://localhost:8080`.
3. **ERP Frontend (Angular):**
   ```bash
   cd frontend && npm install && npm start
   ```
   Disponible en `http://localhost:4200`.
4. **Backoffice Frontend (React):**
   ```bash
   cd backoffice-frontend && npm install && npm run dev
   ```
   Disponible en `http://localhost:5173`.

---

## 🤖 Integración de Agentes y MCP

- **Protocolo Operativo y GitFlow:** [AGENTS.md](AGENTS.md)
- **Herramientas MCP:** [.mcp/README.md](.mcp/README.md) y [.mcp/config.example.json](.mcp/config.example.json)
- **Automatización Chrome DevTools WSL2:** [mcp-chrome-devtools-wsl2.md](docs/lessons_learned/mcp-chrome-devtools-wsl2.md)
