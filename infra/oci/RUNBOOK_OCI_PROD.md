# Runbook de Paso a Producción: Oracle Cloud Infrastructure (`Go-Live`)

> **Prerrequisito:** Haber completado íntegramente [`RUNBOOK_OCI_QA_STAGING.md`](./RUNBOOK_OCI_QA_STAGING.md).  
> Por lo tanto, ya existen y están operativos en OCI (`us-ashburn-1` / `cmp-erp`):
> - Gobernanza CIS (`ops-erp`, `svc-erp-s3`, Budget `$1.00 USD`).
> - VCN Zero-Trust (`vcn-erp`), `VM 1` (`129.80.29.124` / `10.0.0.10`) y `VM 2` (`10.0.1.20` privada).
> - PostgreSQL 16 en `VM 2` con base `erp_prod` (`app_prod`), `pg_hba.conf` estricto y `cron` diario de backup GPG hacia `erp-db-backups`.
> - Buckets S3 `erp-media-prod` y `erp-db-backups`.

Este Runbook cubre **únicamente los pasos diferenciales** para promover el sistema a **Producción 24/7** con dominio propio y nombre definitivo de la aplicación.

---

## 📋 Checklist Rápido de Go-Live Producción

- [ ] **P1. Tareas Pre-Producción cerradas:** `TASK-641` (Lifecycle Policy `> 30d` en `erp-db-backups`), `TASK-642` (Branding/título definitivo desde Backoffice) y `TASK-643` (Auditoría OWASP Top 10 / DAST end-to-end).
- [ ] **P2. Dominio propio y DNS configurados:** Registros `A` (`app`, `admin`, `api`, `logs`) apuntando a `129.80.29.124`.
- [ ] **P3. `.env` de `VM 1` actualizado para Producción:** `IMAGE_TAG=latest`, dominios reales, `CORS_ALLOWED_ORIGINS` y `ACME_EMAIL`.
- [ ] **P4. Variables y Aprobación en GitHub:** `OCI_PROD_DEPLOY_ENABLED=true`, `APP_DOMAIN` definitivo y `Required reviewers` en `Environment: production`.
- [ ] **P5. Liberación GitFlow (`main` + Tag `vX.Y.Z`):** PR hacia `main`, tag SemVer final, aprobación manual en `cd-production.yml`.
- [ ] **P6. Certificación Go-Live y Limpieza Staging:** HTTPS verde, `SuperAdmin` operativo, prueba S3 `erp-media-prod`, *Restore Drill* y apagado de `--profile staging`.

---

## 🛠️ Guía Detallada de Paso a Producción

### PASO P1 — Cerrar Prerrequisitos de Seguridad y Branding (`docs/tasks.md`)
Antes de abrir tráfico a clientes reales:
1. **`TASK-641`:** Activar en `root` la política `Allow service objectstorage-us-ashburn-1 to manage object-family in compartment cmp-erp` y habilitar la regla Lifecycle (`> 30 días`) sobre `erp-db-backups`.
2. **`TASK-642`:** Configurar el nombre comercial definitivo de la aplicación desde Backoffice (`platform_settings`).
3. **`TASK-643`:** Ejecutar el escaneo integral **OWASP Top 10** y pruebas de seguridad de extremo a extremo contra el entorno de QA/Staging antes de promover a `main`.

---

### PASO P2 — Dominio Propio y Configuración DNS
Una vez comprado tu dominio definitivo (ej. `miapp.com` en Cloudflare Registrar, Porkbun, etc.):
1. En el panel DNS de tu proveedor, crea **4 registros `A`** apuntando a la IP pública reservada de `VM 1` (**`129.80.29.124`**):
   - `app` $\rightarrow$ `129.80.29.124` (ERP POS)
   - `admin` $\rightarrow$ `129.80.29.124` (Backoffice SaaS)
   - `api` $\rightarrow$ `129.80.29.124` (Backend Spring Boot)
   - `logs` $\rightarrow$ `129.80.29.124` (Dozzle)
2. **Nota importante si usas Cloudflare:**
   - Inicialmente deja la nube en **Gris (`DNS Only`)** para que Caddy obtenga directamente los certificados Let's Encrypt sin interferencias.
   - Si luego activas proxy naranja en Cloudflare, configura SSL/TLS en modo **`Full (Strict)`** (nunca `Flexible`, o habrá bucles de redirección con Caddy).
3. Verifica resolución desde tu terminal local:
   ```bash
   getent hosts app.<tu-dominio> api.<tu-dominio> admin.<tu-dominio> logs.<tu-dominio>
   ```

---

### PASO P3 — Actualizar `/opt/gestion-restaurante/.env` en `VM 1`
Conéctate a `VM 1` (`ssh oci-vm1`) y edita `/opt/gestion-restaurante/.env`:
1. Cambia **`IMAGE_TAG=staging`** por **`IMAGE_TAG=latest`** *(a partir del primer release a `main`, Producción usará `:latest` y quedará desacoplada de Staging)*.
2. Actualiza los dominios de Producción y CORS:
   ```env
   ACME_EMAIL=ops@<tu-dominio>
   APP_DOMAIN=app.<tu-dominio>
   ADMIN_DOMAIN=admin.<tu-dominio>
   API_DOMAIN=api.<tu-dominio>
   LOGS_DOMAIN=logs.<tu-dominio>
   CORS_ALLOWED_ORIGINS=https://app.<tu-dominio>,https://admin.<tu-dominio>
   ```
3. Verifica que `PROD_DB_NAME=erp_prod`, `S3_BUCKET=erp-media-prod` y `BACKUP_S3_BUCKET=erp-db-backups` estén correctos.
4. *(Opcional recomendado)* Verifica que en `VM 1` siga apagado el stack de Staging si no lo estás usando en ese momento.

---

### PASO P4 — Habilitar Producción en GitHub
En tu repositorio en **GitHub.com $\rightarrow$ `Settings`**:
1. **`Secrets and variables -> Actions -> Variables`:**
   - Cambia **`OCI_PROD_DEPLOY_ENABLED`** de `false` a **`true`**.
   - Actualiza **`APP_DOMAIN`** a `app.<tu-dominio>`.
2. **`Environments -> production`:**
   - Confirma que **`Required reviewers`** está activo con tu usuario.
   - Confirma que los 3 secrets (`OCI_VM1_HOST`, `OCI_VM1_USER`, `OCI_VM1_SSH_KEY`) están cargados.

---

### PASO P5 — Liberación GitFlow (`main` + Tag SemVer Final)
Según [cd-production.yml](../../.github/workflows/cd-production.yml):
1. Integra `develop` $\rightarrow$ `release/sprint-X` $\rightarrow$ **Pull Request hacia `main`** (con CI verde).
2. Una vez aprobado y mergeado el PR en `main`, crea y sube el **tag SemVer final** (sin sufijos `-rc` ni `-staging`):
   ```bash
   git checkout main
   git pull origin main
   git tag -a v1.0.0 -m "Release Producción v1.0.0"
   git push origin v1.0.0
   ```
3. En **GitHub Actions**, el workflow **`CD Production Release`**:
   - Ejecutará `release-quality-gate` (tests, SpotBugs, `npm audit`, devsecops).
   - Compilará y escaneará con Trivy las imágenes `linux/arm64`, publicando `:latest` y `:sha-...` en GHCR.
   - Se detendrá en **`Deploy Production (Manual Approval Required)`** esperando tu clic de aprobación (`Review deployments -> Approve and deploy`).

---

### PASO P6 — Certificación Go-Live y Cierre
1. **Healthcheck HTTPS:** `curl -i https://api.<tu-dominio>/actuator/health` (`HTTP/2 200`, `"status":"UP"`).
2. **Backoffice (`https://admin.<tu-dominio>`):** Inicia sesión con `PLATFORM_ADMIN_EMAIL`, cambia la contraseña inicial si aplica, prueba **Storage $\rightarrow$ Probar conexión S3** contra `erp-media-prod` y configura parámetros globales (`TASK-642`).
3. **ERP (`https://app.<tu-dominio>`):** Realiza el onboarding del primer restaurante real o prueba de humo de venta + subida de imagen.
4. **Restore Drill de verificación (en `VM 2`):** Ejecuta al menos una prueba de restauración sobre una BD temporal (`restore-postgres-s3.sh --target-db erp_restore_drill`) para certificar que tus backups cifrados en `erp-db-backups` son 100% recuperables.
5. **Apagar Staging si quedó levantado:**
   ```bash
   cd /opt/gestion-restaurante
   rm -f docker/caddy/conf.d/staging.caddy
   docker compose -f docker/docker-compose.prod.yml exec caddy caddy reload --config /etc/caddy/Caddyfile
   docker compose -f docker/docker-compose.staging.yml --profile staging down
   ```
