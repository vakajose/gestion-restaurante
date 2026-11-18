# Sistema Integral de Gestión para Restaurante (POS + ERP + Backoffice SaaS)

Plataforma modular multi-tenant para la gestión operativa (POS, Kardex/Recetas, Menús Diarios, Combos, Modificadores, Delivery/Pickup) y financiera (Arqueos, Gastos, Autorizaciones) de restaurantes con arquitectura **offline-first**, acompañada de un **Backoffice SaaS independiente** para administración de plataforma, planes, suscripciones, cuotas y observabilidad.

---

## 🛠️ Stack Tecnológico

- **Backend (`backend/`):** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith 2.1.x, PostgreSQL 16, Flyway (`V1`–`V24`, 56 tablas), Maven.
- **ERP Restaurante (`frontend/`):** Angular 22 (`provideZonelessChangeDetection()`, `ReactiveFormsModule` / `FormsModule` con Signals, Standalone Components), Tailwind CSS 3.4 (TailwindAdmin v3.0 + Solar/Tabler Icons), Dexie.js (IndexedDB / Offline-first).
- **Backoffice SaaS (`backoffice-frontend/`):** React 19 LTS, TypeScript, Vite, React Router, Tailwind CSS 3.4, Lucide React, Vitest + React Testing Library.
- **Infraestructura & Observabilidad (`docker/`):** Docker Multi-Stage, Caddy (Reverse Proxy / TLS), Dozzle (Visor de logs), OCI Object Storage / S3-compatible.

---

## 📂 Estructura del Repositorio

```text
gestion-restaurante/
├── backend/                 # API Spring Boot + Spring Modulith (ERP + Backoffice)
├── frontend/                # SPA ERP Restaurante (Angular 22 + Dexie.js + Playwright E2E)
├── backoffice-frontend/     # SPA Backoffice SaaS (React 19 + Vite + Tailwind CSS)
├── docker/                  # Compose (dev, testing-local, staging, prod), Caddy y VM2 Postgres
├── scripts/                 # Scripts de respaldo/restauración S3, seguridad DevSecOps, E2E y ops
└── docs/                    # 🏛️ Bóveda Obsidian (ADRs, SDD, Sprints, Manuales y Pruebas)
```

---

## 📚 Documentación y Bóveda Obsidian (`docs/`)

La carpeta `docs/` conforma la **Bóveda Unificada de Obsidian** bajo el estándar **Specification-Driven Development (SDD)**:

- **Índice Central (MOC):** [docs/README.md](docs/README.md)
- **Manual de Bootstrap y Operación Inicial:** [bootstrap_y_operacion_inicial.md](docs/manuals/plataforma/bootstrap_y_operacion_inicial.md) *(cómo levantar desde cero, `.env` y primeras configuraciones en Backoffice)*
- **Manuales Operativos:** [Manual de Restaurante (ERP/POS)](docs/manuals/restaurante/README.md) | [Manual de Operación Backoffice](docs/manuals/plataforma/operacion_backoffice_saas.md)
- **Infraestructura Docker y 4 Ambientes:** [docker/README.md](docker/README.md)
- **Sub-Bóveda Backoffice SaaS:** [docs/backoffice/README.md](docs/backoffice/README.md)
- **Backlog Activo (Sprint 6):** [docs/tasks.md](docs/tasks.md) | **Sprints Históricos:** [docs/project/sprints/README.md](docs/project/sprints/README.md)
- **Gobernanza SDD-SDLC:** [ADR-0007](docs/adr/0007_sdd_software_development_lifecycle.md) y [.agents/skills/sdd-architecture-diagramming/SKILL.md](.agents/skills/sdd-architecture-diagramming/SKILL.md)

---

## 🚀 Puesta en Marcha

### Opción A: Entorno Completo Dockerizado (`testing-local`)
Emula la topología completa (`postgres`, `backend`, `erp-frontend`, `backoffice-frontend`, `dozzle`, `caddy`) sin seeds de demostración. Ver guía completa en [bootstrap_y_operacion_inicial.md](docs/manuals/plataforma/bootstrap_y_operacion_inicial.md).

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
   > **Nota de Base de Datos en `dev`:** `docker/docker-compose.yml` crea por defecto la base `restaurant_db`, mientras que `backend/src/main/resources/application-dev.yml` apunta por defecto a `jdbc:postgresql://localhost:5432/restaurant_dev`. Antes de iniciar el backend, crea la base `restaurant_dev` en PostgreSQL o sobrescribe la variable:
   > ```bash
   > export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/restaurant_db
   > ```
2. **Backend (Spring Boot con perfil `dev` y datos demo):**
   ```bash
   cd backend && ./mvnw spring-boot:run
   ```
   Disponible en `http://localhost:8080`.
3. **ERP Frontend (Angular 22):**
   ```bash
   cd frontend && npm install && npm start
   ```
   Disponible en `http://localhost:4200`.
4. **Backoffice Frontend (React 19):**
   ```bash
   cd backoffice-frontend && npm install && npm run dev
   ```
   Disponible en `http://localhost:5173`.

---

## 🧪 Verificación Rápida y DevSecOps

```bash
# Backend (JUnit 5 + Spring Modulith + SpotBugs SAST)
cd backend && ./mvnw -B clean test spotbugs:check -Psecurity

# ERP Frontend (ESLint + Vitest + Build)
cd frontend && npm run lint && npm test -- --watch=false && npm run build

# Backoffice React (ESLint + Vitest + Build)
cd backoffice-frontend && npm run lint && npm test -- --run && npm run build

# Auditoría DevSecOps rápida (secretos y Dockerfiles)
./scripts/security/run-security-scan.sh --quick
```

---

## 🤖 Integración de Agentes y MCP

- **Protocolo Operativo y GitFlow:** [AGENTS.md](AGENTS.md)
- **Herramientas MCP:** [.mcp/README.md](.mcp/README.md) y [.mcp/config.example.json](.mcp/config.example.json)
- **Automatización Chrome DevTools WSL2:** [mcp-chrome-devtools-wsl2.md](docs/lessons_learned/mcp-chrome-devtools-wsl2.md)
