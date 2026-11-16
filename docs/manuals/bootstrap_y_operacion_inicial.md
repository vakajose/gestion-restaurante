---
title: "Manual de Bootstrap desde Cero y Operación Inicial (Backoffice + ERP)"
category: "manuals"
tags:
  - manual
  - bootstrap
  - testing-local
  - staging
  - backoffice
  - onboarding
last_updated: "2026-11-13"
status: "verified"
---

# 🚀 Manual de Bootstrap desde Cero y Operación Inicial

> ⬅️ **Navegación:** [README Principal](../../README.md) | [Infraestructura Docker (`docker/README.md`)](../../docker/README.md) | [Sub-Bóveda Backoffice](../backoffice/README.md)

Esta guía describe paso a paso cómo levantar la plataforma **desde cero** (con base de datos vacía), qué variables `.env` aplican por contenedor y qué configuraciones iniciales debes revisar en el **Backoffice** antes de operar o probar el **ERP Restaurante**.

---

## 1. Variables de Entorno (`.env`) por Contenedor y Ambiente

### ¿Qué contenedores leen `.env` y cuáles no?

En la topología contenerizada ([`docker-compose.testing-local.yml`](../../docker/docker-compose.testing-local.yml)):

| Servicio | ¿Requiere `.env`? | Motivo / Variables clave |
| :--- | :--- | :--- |
| **`postgres`** | Opcional en `testing-local` / **Obligatorio en Nube** | Lee `TESTING_DB_NAME`, `TESTING_DB_USER`, `TESTING_DB_PASSWORD` (tienen defaults en `testing-local`). |
| **`backend`** | Opcional en `testing-local` / **Obligatorio en Nube** | Lee credenciales BD, `JWT_SECRET`, `PLATFORM_JWT_SECRET`, `PLATFORM_ADMIN_*` y `APP_STORAGE_TYPE` (`LOCAL` o `S3_*`). |
| **`erp-frontend`** | **No** | El compose/despliegue le inyecta `API_BASE_URL` en runtime (`env.js`). |
| **`backoffice-frontend`** | **No** | El compose/despliegue le inyecta `API_BASE_URL` en runtime (`env.js`). |
| **`dozzle`** | **No** | Visor de logs de solo lectura conectado al socket Docker. |
| **`caddy`** | **No** | Reverse proxy configurado por `Caddyfile.testing-local` / `Caddyfile.cloud`. |

> [!IMPORTANT]
> - En **`testing-local`**, todos los valores tienen defaults seguros para localhost; no es obligatorio crear un `.env`.
> - En **`staging`** y **`prod`**, **nunca se commitea `.env` al repositorio**: los secretos se almacenan en **GitHub Actions Secrets** (por *Environment*) o en el `.env` de la VM basado en [`docker/.env.cloud.example`](../../docker/.env.cloud.example).

---

## 2. Arranque Limpio desde Cero (`testing-local`)

### Prerrequisito: Resolución local de subdominios
Verifica que `/etc/hosts` contenga:
```text
127.0.0.1 app.localhost admin.localhost api.localhost logs.localhost
```

### Levantar el stack
```bash
docker compose -f docker/docker-compose.testing-local.yml up --build -d
```

### ¿Qué ocurre automáticamente durante el bootstrap?
1. **`postgres`:** Arranca sobre `tmpfs` (512 MB, efímero), crea automáticamente la base de datos `restaurant_testing` y pasa `pg_isready`.
2. **`backend`:** Espera a que `postgres` esté `healthy`, ejecuta todas las migraciones **Flyway (`V1`–`V19`)**, crea el usuario inicial **`superadmin`** y expone `/actuator/health` como `UP`.
   - **Sin datos demo:** Los inicializadores de prueba (`DataInitializer`, `CatalogDataInitializer`, `FinanceDataInitializer`, `InventoryDataInitializer`) están aislados bajo `@Profile({"dev", "test"})`, por lo que en `testing-local`, `staging` y `prod` **no se crean restaurantes ni usuarios demo**.
3. **`erp-frontend`, `backoffice-frontend` y `dozzle`:** Se construyen e inician sirviendo sus SPAs y visor de logs.
4. **`caddy`:** Espera a `backend` (`healthy`) y abre el puerto `80`.

### URLs disponibles al finalizar
- **ERP Restaurante (Angular):** `http://app.localhost`
- **Backoffice Plataforma (React):** `http://admin.localhost`
- **API Healthcheck:** `http://api.localhost/actuator/health`
- **Logs en vivo (Dozzle):** `http://logs.localhost`

---

## 3. Primeras Configuraciones en Backoffice (`http://admin.localhost`)

Antes de crear o probar restaurantes en el ERP, ingresa al **Backoffice** con el usuario bootstrap:
- **Usuario (`testing-local` default):** `superadmin`
- **Contraseña (`testing-local` default):** `SuperAdminLocal123!`

*(En `staging`/`prod` usa las credenciales definidas en `PLATFORM_ADMIN_*`).*

### Checklist obligatorio de revisión inicial

1. **Perfil y Seguridad (`/profile` y `/platform/admins`):**
   - Si el sistema solicita cambio de contraseña (`must_change_password`), actualízala en `/profile`.
   - En `/platform/admins` (solo `SUPERADMIN`), crea operadores adicionales (`PLATFORM_OPERATOR` o `SUPERADMIN`) si requieres probar roles diferenciados.

2. **Storage y Parámetros Globales (`/platform/settings`):**
   - Verifica los parámetros sembrados por `V18`:
     - `storage.active_provider`: `LOCAL` en `testing-local` (o `S3` en nube OCI).
     - `storage.max_file_size_bytes`: `1048576` (1 MB por defecto; ajustable entre 100 KB y 10 MB).
     - `storage.public_url_prefix`: `/uploads/dishes/` (para almacenamiento `LOCAL`).
   - **Nota operativa por ambiente para imágenes:**
     - Con `LOCAL`, el ERP resuelve automáticamente `/uploads/...` contra `API_BASE_URL` y Caddy también enruta `/uploads/*` hacia el `backend`, por lo que las fotos funcionan en `dev`, `testing-local` (`http://app.localhost` y `http://localhost`), `staging` y `prod`.
     - Si cambias `storage.active_provider` a `S3` (OCI Object Storage / CDN), debes configurar `storage.public_url_prefix` con la URL pública absoluta del bucket/CDN (ej. `https://objectstorage.<region>.oraclecloud.com/n/<ns>/b/<bucket>/o/`) o su proxy público correspondiente, y confirmar las credenciales S3 en variables de entorno antes de activar `S3`.
   - Presiona **"Probar Conexión"** para confirmar que el motor de almacenamiento responde antes de subir fotos de platillos en el ERP.

3. **Planes de Suscripción (`/plans`):**
   - Revisa los 4 planes base sembrados por `V19`:
     - `TRIAL_14D`: Plan automático asignado al onboarding de nuevos restaurantes (14 días, 0 BOB, 3 sucursales, 15 usuarios, 100 platillos, 200 insumos, 2000 pedidos/mes).
     - `WEEKLY_FLEX`, `MONTHLY_STANDARD` y `MONTHLY_PRO`: Planes comerciales editables.
   - Ajusta límites o crea planes personalizados si vas a probar escenarios de cuotas o vencimientos.

---

## 4. Primer Onboarding en ERP y Verificación

1. Abre **`http://app.localhost`** y ejecuta el flujo de **Onboarding** (*"Registra tu empresa"*) para dar de alta tu primer restaurante, sucursal y usuario administrador.
2. Vuelve al **Backoffice** (`http://admin.localhost`):
   - En **`/tenants`** verifica que el restaurante aparece registrado con su suscripción `TRIAL_14D`.
   - En **`/tenants/:id`** puedes probar cambios de plan, registro de pagos/cargos, periodos de gracia, *overrides* de cuota o suspensiones (`POS_AND_SHIFT_ONLY` / `TOTAL`).
   - En **`/platform/observability`** verifica salud de BD/Hikari, loggers en caliente y bitácora de auditoría.

---

## 5. Recuperación de Emergencia (*Break-Glass*)

Si pierdes acceso al usuario `superadmin`, utiliza el script de recuperación CLI:
```bash
./scripts/ops/reset-superadmin.sh
```
