# Infraestructura Docker, Staging On-Demand y Producción OCI (ADR-0026)

Este directorio contiene la topología completa de contenedores para los entornos dockerizados del sistema (`testing-local`, `staging-nube` y `prod`) manteniendo separación estricta entre **VM 1 (Aplicaciones + Reverse Proxy Caddy + Dozzle)** y **VM 2 (PostgreSQL 16)**.

---

## 1. Mapa de Archivos

| Archivo | Propósito |
| :--- | :--- |
| `docker-compose.yml` | PostgreSQL 16 local persistente exclusivamente para desarrollo nativo (`dev` en IDE). |
| `docker-compose.testing-local.yml` | Emulación local completa (`Caddy` + `Backend` + `Nginx ERP` + `Nginx Backoffice` + `Dozzle` + `PostgreSQL` efímero) a costo `$0`. |
| `docker-compose.prod.yml` | Stack VM 1 de Producción 24/7 (sin BD local; conecta por VCN privada a VM 2). |
| `docker-compose.staging.yml` | Stack **Staging Efímero On-Demand** mediante Compose `profiles: ["staging"]` (apagado por defecto = `0 MB` RAM). |
| `caddy/Caddyfile.testing-local` | Enrutamiento HTTP local (`app.localhost`, `admin.localhost`, `api.localhost`, `logs.localhost`). |
| `caddy/Caddyfile.cloud` | Proxy reverso TLS automático (Let's Encrypt) + `basic_auth` sobre Dozzle, **sin montar `/var/run/docker.sock`**. |
| `caddy/conf.d/staging.caddy.template` | Plantilla de subdominios staging protegidos con `basic_auth` (copiar a `staging.caddy` al activar staging). |
| `postgres-vm2/*` | Tuning `postgresql.conf`, plantilla `pg_hba.conf` Zero-Trust e `init-databases-and-roles.sql` para `restaurant_prod` y `restaurant_staging`. |
| `.env.cloud.example` | Plantilla Twelve-Factor de variables obligatorias para despliegue nube. |

---

## 2. Rotación Estricta de Logs (Prevención `Disk Full`)

1. **Nivel Docker Engine / Compose:** Todos los servicios declaran `driver: json-file` con `max-size: "10m"` y `max-file: "5"` (`<= 50 MB` máx. por contenedor).
2. **Nivel Spring Boot (`backend/src/main/resources/logback-spring.xml`):** En perfiles `testing-local`, `staging` y `prod`, `ROLLING_FILE` rota cada `10MB` con compresión `.gz`, retención máxima de `30` días y `totalSizeCap` global de `500MB`.

---

## 3. Operación por Entorno

### A. Desarrollo diario (`dev` nativo)
```bash
docker compose -f docker/docker-compose.yml up -d
```

### B. Testing Local End-to-End (`testing-local`)
```bash
docker compose -f docker/docker-compose.testing-local.yml up --build -d
# ERP:        http://app.localhost
# Backoffice: http://admin.localhost
# API Health: http://api.localhost/actuator/health
# Dozzle:     http://logs.localhost
```

### C. Producción (VM 1 + VM 2)
```bash
cp docker/.env.cloud.example .env
# Completar secretos y dominios en .env
docker compose -f docker/docker-compose.prod.yml --env-file .env up -d
```

### D. Staging Efímero On-Demand (VM 1)
```bash
cp docker/caddy/conf.d/staging.caddy.template docker/caddy/conf.d/staging.caddy
docker compose -f docker/docker-compose.prod.yml -f docker/docker-compose.staging.yml --profile staging --env-file .env up -d
docker compose -f docker/docker-compose.prod.yml exec caddy caddy reload --config /etc/caddy/Caddyfile

# Al finalizar la certificación QA (libera 100% RAM/CPU):
rm -f docker/caddy/conf.d/staging.caddy
docker compose -f docker/docker-compose.prod.yml exec caddy caddy reload --config /etc/caddy/Caddyfile
docker compose -f docker/docker-compose.staging.yml --profile staging down
```

---

## 4. Respaldos Automatizados y Restore Drill (VM 2 -> Object Storage)

Ver scripts [`scripts/db/backup-postgres-s3.sh`](../scripts/db/backup-postgres-s3.sh) y [`scripts/db/restore-postgres-s3.sh`](../scripts/db/restore-postgres-s3.sh):

```bash
# Respaldo cifrado AES-256 + subida a OCI Object Storage (cron diario recomendado en VM 2: 03:00 UTC)
BACKUP_ENCRYPTION_PASSPHRASE="..." S3_ENDPOINT="..." BACKUP_S3_BUCKET="restaurant-db-backups" \
  ./scripts/db/backup-postgres-s3.sh

# Prueba de restauración segura sobre base temporal (Restore Drill)
BACKUP_ENCRYPTION_PASSPHRASE="..." \
  ./scripts/db/restore-postgres-s3.sh --file /tmp/restaurant-backups/restaurant_prod_YYYYMMDDTHHMMSSZ.sql.gz.gpg --target-db restaurant_restore_drill
```
