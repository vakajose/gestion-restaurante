# ADR-0026: Estrategia de Infraestructura Cloud, Despliegue y Separación de Base de Datos

- **Estado:** Aceptado
- **Fecha:** 2026-11-12
- **Sprint / Tarea:** Sprint 6 — `TASK-601` (`PB-025`)
- **Autores:** Equipo de Arquitectura (Co-diseño interactivo)

---

## 1. Contexto

El sistema entra en fase de preparación para despliegue en nube (`Sprint 6`) con una escala inicial proyectada de **$\ge 10$ clientes** y operación a cargo de **1 solo mantenedor**. La arquitectura está compuesta por:
- **Backend:** Monolito modular en `Java 25` + `Spring Boot 4.1.x` (`Spring Modulith`, Virtual Threads habilitados) + Flyway + Actuator.
- **Frontends:** Dos SPAs estáticas independientes (`Angular 22` ERP POS offline-first con `Dexie.js` y `React LTS` Backoffice administrativo).
- **Base de Datos:** `PostgreSQL 16`.

Se requiere una infraestructura segura, mantenible, con separación física entre el servidor de aplicaciones y la base de datos, respaldos duraderos externos, soporte para entorno de **Staging** sin duplicar costos, y un balance óptimo de costo/rendimiento.

---

## 2. Decisión

Se adopta el stack **OCI Sovereign Stack (Pay-As-You-Go dentro de Always Free)** bajo el siguiente diseño:

### 2.1 Proveedor y Topología de Red (2 VMs en Red Privada VCN)
- **Proveedor:** Oracle Cloud Infrastructure (OCI), cuenta actualizada a **Pay-As-You-Go (PAYG)** para quedar exenta de la política de reclamación por inactividad (*Idle Reclamation*), operando dentro de los límites **Always Free** de cómputo Ampere A1 ARM64 (`2 OCPUs`, `12 GB RAM`, `200 GB Block Storage`, `20 GB Object Storage`, `10 TB egress/mes`).
- **VM 1 — Servidor de Aplicaciones (`1 OCPU / 8 GB RAM`):**
  - Ejecuta `Docker Engine` + `Docker Compose`.
  - **Proxy Reverso:** `Caddy Server` (~25–40 MB RAM), configurado mediante `Caddyfile` declarativo, TLS automático (Let's Encrypt) y **sin acceso al socket de Docker** (`/var/run/docker.sock`) por principio de mínimo privilegio.
  - **Observabilidad liviana:** `Dozzle` (~15 MB RAM) protegido por autenticación/IP para visualización de logs en tiempo real.
  - **Aplicaciones:** Contenedor Backend (`Spring Boot`) + 2 contenedores `Nginx` (`Angular ERP` y `React Backoffice`) con inyección de variables en runtime (`env.js`).
- **VM 2 — Servidor de Base de Datos (`1 OCPU / 4 GB RAM`):**
  - Exclusiva para `PostgreSQL 16`.
  - **Aislamiento Zero-Trust:** Sin IP pública expuesta para el puerto `5432`; accesible únicamente desde la IP privada de la VM 1 dentro de la VCN.
  - **Respaldos Duraderos:** Automatización de `pg_dump` comprimido y encriptado enviado a **OCI Object Storage** (API compatible S3).

### 2.2 Matriz de 4 Entornos y Estrategia de Staging
Se formalizan **4 entornos** con responsabilidades estrictamente separadas:

| Entorno | Dónde corre | Base de Datos | Propósito |
| :--- | :--- | :--- | :--- |
| **`dev`** | Local nativo (IDE / hot-reload) | `restaurant_dev` | Desarrollo diario rápido con datos demo (`@Profile("dev")`). |
| **`testing-local`** | Local dockerizado (`Docker Compose` + `Caddy` + `Nginx` + `Backend`) | `restaurant_testing` (efímera, creada de cero en cada levantamiento) | Emular fielmente la topología de nube en local: validar `Dockerfile`s, `env.js`, `Caddyfile` y migraciones Flyway limpias a costo `$0`. |
| **`staging-nube`** | OCI VM 1 + VM 2 (bajo demanda) | `restaurant_staging` (rol `app_staging`) | Certificación pre-producción en nube real con TLS y red privada VCN. |
| **`prod`** | OCI VM 1 + VM 2 (24/7) | `restaurant_prod` (rol `app_prod`) | Operación productiva de clientes. |

- **Staging Efímero On-Demand (`staging-nube`):** Coexiste en la nube usando **Docker Compose Profiles** (`profiles: ["staging"]`), **apagado por defecto** (`$0.00` de costo y `0 MB` de RAM en operación normal). Se activa únicamente para certificar releases (`docker compose --profile staging up -d`) y se apaga al finalizar (`down`).
- **Aislamiento de Datos en VM 2:** Bases de datos separadas (`restaurant_prod` y `restaurant_staging`) con **roles/usuarios independientes** (`app_prod` y `app_staging`) para impedir cualquier acceso cruzado.
- **Enrutamiento:** Subdominios dedicados en Caddy (`staging-api.*`, `staging-app.*`, `staging-admin.*`) apuntando a puertos internos separados y protegidos con autenticación básica / restricción de acceso.

### 2.3 Dimensionamiento Explícito de Pools de Conexiones
Dado el uso de **Virtual Threads** en Java 25, se prohíbe depender de valores implícitos:
- **Backend Producción (`HikariCP`):** `maximum-pool-size: 15`, `minimum-idle: 5`, `connection-timeout: 10000`, `idle-timeout: 300000`, `max-lifetime: 1800000`, `leak-detection-threshold: 15000`.
- **Backend Staging (`HikariCP`):** `maximum-pool-size: 5`, `minimum-idle: 1`.
- **PostgreSQL 16 (`postgresql.conf` en VM 2 — 4 GB RAM):** `max_connections = 50`, `shared_buffers = 1GB`, `effective_cache_size = 3GB`, `work_mem = 16MB`. No se requiere `PgBouncer` mientras exista una única instancia monolítica del backend.

---

## 3. Consecuencias

### Positivas
- **Costo mensual base:** `$0.00 USD/mes` dentro de los límites Always Free en cuenta PAYG, con capacidad real (12 GB RAM totales) superior a ofertas pagas equivalentes de `$35–$80 USD/mes`.
- **Seguridad y aislamiento:** Base de datos físicamente separada sin exposición pública, proxy reverso sin acceso al socket Docker, y entornos `prod`/`staging` aislados por credenciales.
- **Portabilidad:** Todo el stack corre en contenedores Docker estándar (`linux/arm64`), permitiendo migrar a cualquier otro proveedor IaaS sin reescribir la arquitectura.

### Negativas / Riesgos Mitigados
- **Arquitectura ARM64:** Todos los `Dockerfile` y pipelines CI/CD deberán compilar imágenes compatibles con `linux/arm64` (o multi-arch).
- **Mantenimiento de SO:** Al usar IaaS puro sin PaaS visual (Coolify/Dokploy), las actualizaciones de seguridad del host Linux y la automatización de respaldos se gestionan vía scripts/CI-CD.
