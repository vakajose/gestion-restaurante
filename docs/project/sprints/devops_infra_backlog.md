---
title: "Backlog de Infraestructura, Contenerización y Despliegue Cloud (DevOps)"
category: "sprints"
sprint: "devops"
status: "in-backlog"
last_updated: "2026-10-28"
---

# ☁️ Backlog de Infraestructura, Contenerización y Despliegue Cloud (DevOps)

> **Estado:** `EN BACKLOG / EJECUCIÓN TRANSVERSAL A DEMANDA`  
> **Objetivo:** Empaquetado en contenedores Docker de grado producción, orquestación de pruebas E2E en ambiente containerizado, aprovisionamiento en la nube (VPS / Oracle Cloud Always Free), certificados SSL/TLS, backups programados de PostgreSQL y pipeline automatizado de CI/CD con GitHub Actions.

---

## Tareas de Infraestructura y Despliegue

- [ ] **TASK-301 [DEVOPS] [INFRA]:** Multi-Stage Dockerfiles para Backend (Java 25) y Frontend (Angular 22) + Nginx SPA Gateway.
  - *Categoría:* DevOps & Contenerización | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear `backend/Dockerfile` multi-stage optimizado (compilación Maven + runtime JRE Eclipse Temurin 25 Alpine recortado con flags de contención `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`) y `frontend/Dockerfile` multi-stage (Node 22 build + Nginx Alpine) configurando proxy pass de `/api/*` hacia el backend y resolución de rutas SPA (`try_files $uri $uri/ /index.html;`).
  - *Criterio de Entrega:* Imágenes Docker generadas localmente, con peso inferior a 200MB en backend y menos de 30MB en frontend.

- [ ] **TASK-302 [CONFIG] [SEGURIDAD]:** Externalización de Configuración y Seguridad (12-Factor App & Actuator Health).
  - *Categoría:* Seguridad & Configuración Cloud | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Parametrizar orígenes CORS en `SecurityConfig` desde variable de entorno (`CORS_ALLOWED_ORIGINS`), obligar inyección de clave JWT segura (`JWT_SECRET`) evitando el fallback por defecto en perfiles de producción, e incorporar endpoints de salud `spring-boot-starter-actuator` (`/actuator/health/liveness`, `/actuator/health/readiness`) para monitoreo y reinicio automático de contenedores.
  - *Criterio de Entrega:* Perfil Spring `prod` activable con validación estricta de variables obligatorias y probes de salud respondiendo `UP`.

- [ ] **TASK-303 [QA] [CALIDAD]:** Orquestación y Ambiente de Certificación Local (`docker-compose.staging.yml`).
  - *Categoría:* QA Infraestructura & Certificación | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear un stack completo reproducible en local mediante `docker/docker-compose.staging.yml` que levante: 1) Base de datos PostgreSQL con migraciones Flyway automáticas, 2) Backend Spring Boot, 3) Frontend con Nginx sirviendo estáticos y ruteando la API en el puerto 80/443. Ejecutar las 25 pruebas E2E de Playwright apuntando contra este entorno simulado en contenedor.
  - *Criterio de Entrega:* El sistema completo levanta con un solo comando (`docker compose -f docker/docker-compose.staging.yml up -d`) y supera el 100% de las pruebas E2E contra contenedores.

- [ ] **TASK-304 [STORAGE] [ARQUITECTURA]:** Abstracción de Almacenamiento de Evidencias Digitales (S3 / Object Storage).
  - *Categoría:* Cloud Storage & Stateless Backend | *Prioridad:* `P2 - Media`
  - *Objetivo:* Implementar una abstracción de almacenamiento (`StorageService` unificada con `PB-035`) para comprobantes de gastos (`evidence_url`), fotos y logos, desacoplando el sistema de archivos del contenedor mediante un cliente compatible con S3 (ej. Cloudflare R2 u OCI Object Storage) para garantizar persistencia y portabilidad sin estado.
  - *Criterio de Entrega:* Subida de comprobante de gasto en `/api/v1/expenses` guarda la URL del bucket y no escribe en el disco local de la aplicación.

- [ ] **TASK-305 [DEVOPS] [INFRA]:** Despliegue en Preproducción / Producción en la Nube (VPS / Oracle Cloud Always Free).
  - *Categoría:* Rollout & Operaciones | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Aprovisionar la instancia de servidor (Oracle Cloud ARM Ampere Free Tier o VPS Linux x86), configurar firewall estricto (UFW / OCI Security Lists abriendo solo puertos 22 con SSH Key, 80 y 443), aprovisionar certificados SSL/TLS automáticos con Let's Encrypt (vía Nginx/Certbot o Caddy) y orquestar el despliegue con Docker Compose.
  - *Criterio de Entrega:* Aplicación accesible públicamente mediante dominio/subdominio HTTPS con certificados válidos y base de datos con aislamiento de red no expuesta a internet.

- [ ] **TASK-306 [DEVOPS] [INFRA]:** Automatización de Respaldos (Backups) y Restauración de Base de Datos PostgreSQL.
  - *Categoría:* Continuidad de Negocio | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Configurar tarea programada (cron job) diaria en el servidor host que ejecute `pg_dump` con compresión de la base de datos `restaurant_db`, cifre el volcado y lo transfiera a un almacenamiento externo seguro (Object Storage/bucket S3 independiente) con política de retención de 30 días, documentando el procedimiento de restauración (`pg_restore`).
  - *Criterio de Entrega:* Script de backup automatizado probado y procedimiento de disaster recovery verificado con una restauración exitosa en un entorno limpio.

- [ ] **TASK-307 [DEVOPS] [INFRA]:** Pipeline de Integración y Despliegue Continuo (CI/CD) hacia Oracle Cloud.
  - *Categoría:* CI/CD & Automatización DevOps | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear workflow `.github/workflows/deploy.yml` que, ante un push o merge a la rama de lanzamiento (`main` o tags de release), compile los artefactos, construya y empaquete las imágenes Docker (publicándolas en GitHub Packages / GHCR o transfiriéndolas directamente vía SSH), conecte de forma segura al host de Oracle Cloud usando GitHub Secrets (`SSH_PRIVATE_KEY`, `HOST`, `USER`) y ejecute la actualización con downtime mínimo (`docker compose pull && docker compose up -d`).
  - *Criterio de Entrega:* Despliegue 100% automatizado y sin intervención manual tras la aprobación del código en GitHub, con tiempo de ejecución dentro de los minutos gratuitos de GitHub Actions.
