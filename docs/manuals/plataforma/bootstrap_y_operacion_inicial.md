---
title: "Manual de Bootstrap, Entornos y Operación Inicial de Plataforma"
category: "platform-manual"
module: "backoffice"
tags:
  - manual
  - plataforma
  - bootstrap
  - docker
  - backoffice
  - go-live
  - s3
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 🚀 Manual de Bootstrap, Entornos y Operación Inicial

> ⬅️ **Índice:** [Bóveda Central (`docs/README.md`)](../../README.md) | [Sub-Bóveda Backoffice](../../backoffice/README.md) | [Operación Backoffice SaaS](./operacion_backoffice_saas.md) | [Manuales Restaurante](../restaurante/README.md)

Guía técnica para desplegar, inicializar de forma limpia (sin inquilinos ni ventas ficticias), recuperar accesos de emergencia y realizar el primer arranque del **ERP Restaurante + Backoffice SaaS** (esquema de base de datos consolidado **`V1`–`V24`**).

---

## 1. Arquitectura de Inicialización Limpia vs Desarrollo

El sistema distingue entre un entorno de desarrollo con datos de ejemplo y los entornos de certificación o producción:

| Perfil Spring (`SPRING_PROFILES_ACTIVE`) | Comportamiento de Bootstrap (`V1`–`V24`) | Uso recomendado |
| :--- | :--- | :--- |
| **`dev`** | Crea un restaurante demo con catálogo de muestra, 4 planes SaaS base y administrador `superadmin` (`SuperAdminDev123!`). | Desarrollo diario en IDE local. |
| **`testing-local` / `staging` / `prod`** | **Arranque 100% limpio:** no inserta restaurantes ni usuarios de restaurante. Solo ejecuta migraciones `V1`–`V24`, siembra el catálogo de planes SaaS y crea el primer `SUPERADMIN` desde variables de entorno. | QA en Docker, Staging y Producción OCI. |

> [!IMPORTANT]
> En perfiles estrictos (`testing-local`, `staging`, `prod`), el servidor aplica **fail-fast**: si falta `PLATFORM_ADMIN_USERNAME`, `PLATFORM_ADMIN_EMAIL` o `PLATFORM_ADMIN_PASSWORD` (mínimo 12 caracteres) cuando la tabla de administradores está vacía, la aplicación aborta el arranque antes de abrir tráfico HTTP.

---

## 2. Variables de Entorno Obligatorias (`.env`)

Antes de desplegar `testing-local`, `staging` o `prod`, parte de [`docker/.env.cloud.example`](../../../docker/.env.cloud.example) y configura secretos únicos:

| Grupo | Variables Clave | Notas Operativas |
| :--- | :--- | :--- |
| **Identidad Plataforma (`SUPERADMIN`)** | `PLATFORM_ADMIN_USERNAME`, `PLATFORM_ADMIN_EMAIL`, `PLATFORM_ADMIN_PASSWORD` | La contraseña inicial exige mínimo **12 caracteres**. Al ingresar por primera vez se fuerza cambio de contraseña (`must_change_password=true`). |
| **Firmas JWT Independientes** | `JWT_SECRET`, `PLATFORM_JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `PLATFORM_JWT_EXPIRATION_SECONDS` | Secretos distintos (≥ 32 bytes aleatorios cada uno) para aislar sesiones ERP de sesiones Backoffice. |
| **Base de Datos PostgreSQL 16** | `DB_PRIVATE_HOST`, `DB_PORT`, `PROD_DB_NAME`, `PROD_DB_USER`, `PROD_DB_PASSWORD` (y homólogas `STAGING_*`) | Flyway ejecuta automáticamente hasta `V24__widen_daily_menus_day_of_week.sql`. |
| **Almacenamiento Multimedia** | `APP_STORAGE_TYPE` (`LOCAL` o `S3`), `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_PATH_STYLE_ACCESS`, `S3_PUBLIC_BASE_URL` | `LOCAL` guarda imágenes 4:3 en disco local (`/app/uploads/dishes/`); `S3` conecta con OCI Object Storage o bucket S3 compatible. |
| **Dominios, TLS & CORS** | `ACME_EMAIL`, `APP_DOMAIN`, `ADMIN_DOMAIN`, `API_DOMAIN`, `LOGS_DOMAIN`, `CORS_ALLOWED_ORIGINS` | En `testing-local`, Caddy expone `app.localhost`, `admin.localhost`, `api.localhost` y `logs.localhost`. |
| **Dozzle & Staging Auth** | `DOZZLE_ADMIN_USER`, `DOZZLE_ADMIN_PASSWORD_HASH`, `STAGING_BASIC_AUTH_USER`, `STAGING_BASIC_AUTH_HASH` | Hashes bcrypt generados con `caddy hash-password`; en archivos `.env` de Compose cada `$` se escapa como `$$`. |
| **Respaldos S3 Cifrados** | `BACKUP_S3_BUCKET`, `BACKUP_ENCRYPTION_PASSPHRASE`, `BACKUP_RETENTION_DAYS` | Usados por [`scripts/db/backup-postgres-s3.sh`](../../../scripts/db/backup-postgres-s3.sh) y [`scripts/db/restore-postgres-s3.sh`](../../../scripts/db/restore-postgres-s3.sh). |

---

## 3. Puesta en Marcha por Entorno

### Opción A: Entorno Completo `testing-local` (Docker)

Emula la topología cloud completa en tu máquina (PostgreSQL efímero, Backend Spring Boot, ERP Angular, Backoffice React, Caddy y visor Dozzle):

```bash
docker compose -f docker/docker-compose.testing-local.yml up --build -d
```

Accesos locales predeterminados:
- **ERP Restaurante (Clientes y Cajeros):** `http://app.localhost`
- **Backoffice SaaS (Plataforma):** `http://admin.localhost` (usuario `superadmin` / contraseña `SuperAdminLocal123!`)
- **API Health Check:** `http://api.localhost/actuator/health`
- **Visor de Logs (Dozzle):** `http://logs.localhost`

### Opción B: Desarrollo Nativo (`dev`)

1. Levantar PostgreSQL local: `docker compose -f docker/docker-compose.yml up -d`
2. Backend: `cd backend && ./mvnw spring-boot:run` (`http://localhost:8080`)
3. ERP Frontend: `cd frontend && npm install && npm start` (`http://localhost:4200`)
4. Backoffice Frontend: `cd backoffice-frontend && npm install && npm run dev` (`http://localhost:5173`)

### Opción C: Producción y Staging en OCI

Consulta el detalle de topología VM1/VM2, comandos de arranque y activación efímera de staging en [`docker/README.md`](../../../docker/README.md).

---

## 4. Checklist del Primer Arranque en el Backoffice

Una vez arriba el stack, completa estos 5 pasos en **`admin.localhost`** (o tu `ADMIN_DOMAIN`):

1. **Primer ingreso y rotación de clave:** Inicia sesión con el `PLATFORM_ADMIN_USERNAME` configurado. Por seguridad, el sistema redirige automáticamente a **`Mi Perfil de Plataforma`** (`/profile`) exigiendo cambiar la clave inicial por una nueva contraseña personal (mínimo 12 caracteres por defecto).
2. **Verificar Storage y Política de Claves (`Storage & Config`):**
   - Revisa `storage.active_provider` (`LOCAL` o `S3`), `storage.max_file_size_bytes` (`1048576` = 1 MB por defecto) y `storage.public_url_prefix` (`/uploads/dishes/` en local o prefijo CDN/bucket en nube) heredados de `V18`.
   - Ejecuta **`Probar Conexión (Proveedor Activo)`** para confirmar escritura/lectura en disco o bucket antes de que los restaurantes suban fotos de platos.
   - Revisa las políticas introducidas en **`V22`**: `security.platform_password_min_length` (`12` por defecto) y `security.platform_password_require_complexity` (`false` por defecto).
3. **Revisar el Catálogo de Planes SaaS (`Planes SaaS`, `V21`):**
   - El arranque siembra automáticamente 4 planes base:
     - `TRIAL_14D`: *Prueba Inicial 14 Días* (`TRIAL`, 14 días, gratis, 3 sucursales, 15 usuarios, 100 platos, 200 insumos, 2.000 órdenes/mes).
     - `WEEKLY_FLEX`: *Plan Semanal Flexible* (`PUBLIC_ONBOARDING`, 7 días, Bs. 75,00, 1 sucursal, 8 usuarios, 60 platos, 120 insumos, 1.200 órdenes/mes).
     - `MONTHLY_STANDARD`: *Plan Mensual Estándar* (`PUBLIC_ONBOARDING`, 30 días, Bs. 250,00, 2 sucursales, 15 usuarios, 150 platos, 300 insumos, 5.000 órdenes/mes).
     - `MONTHLY_PRO`: *Plan Mensual Multi-Sucursal* (`PUBLIC_ONBOARDING`, 30 días, Bs. 550,00, 10 sucursales, 60 usuarios, platos/insumos/órdenes ilimitados).
   - Gracias a **`V21`**, puedes crear planes adicionales en 3 categorías (`PUBLIC_ONBOARDING`, `TRIAL` o `CUSTOM_PRIVATE` visibles solo desde Backoffice) y definir **duración `0` días = Sin vencimiento**, o usar el botón **`Preset: Plan Gratis Ilimitado`**.
4. **Incorporar Operadores de Plataforma (`Admins Plataforma`, `V22`):**
   - Si trabajarás con soporte comercial o cobranzas, crea cuentas con rol `PLATFORM_OPERATOR`. Desde `V22`, el correo electrónico es **opcional** al dar de alta administradores de plataforma desde esta pantalla; el nuevo operador deberá cambiar su contraseña temporal al primer ingreso.
5. **Dar de alta el primer Restaurante (`Inquilinos` o Onboarding Público):**
   - Los clientes pueden registrarse solos desde `/onboarding` en el ERP (3 pasos + plantilla operativa), o puedes pre-aprovisionarlos desde Backoffice en **`Inquilinos`** → **`Nuevo Inquilino`**.
   - Gracias a **`V20`**, todo inquilino recién pre-aprovisionado mostrará el distintivo **`Sin primer ingreso`** (`Primer ingreso: Pendiente` en su ficha) hasta que su dueño o personal complete la primera autenticación real en el ERP.

> [!NOTE]
> El esquema **`V23`–`V24`** ya deja operativas en base de datos las **jornadas de servicio compartidas por sucursal** (`Toda la Carta` vs `Menú del Día`), los **combos vinculados al Menú del Día** con ajuste de precios por selección y las **plantillas de Menú del Día multi-día**, listas para que cada restaurante las opere desde el ERP sin configuración adicional en plataforma.

---

## 5. Recuperación de Emergencia (`Break-Glass`)

Si el `SUPERADMIN` pierde acceso o queda bloqueado, utiliza el script [`scripts/ops/reset-superadmin.sh`](../../../scripts/ops/reset-superadmin.sh) por SSH sobre el host Docker:

```bash
PLATFORM_ADMIN_USERNAME=superadmin ./scripts/ops/reset-superadmin.sh
```

Qué hace internamente:
1. Exige una nueva contraseña temporal de al menos 12 caracteres (la solicita por terminal interactiva sin eco si no se pasa `PLATFORM_ADMIN_PASSWORD`).
2. Ejecuta el contenedor backend en modo único de rescate (`break-glass-reset=true`, `exit-after-reset=true`, sin levantar puerto HTTP).
3. Actualiza el hash de contraseña, reactiva la cuenta, revoca todas sus sesiones activas y marca `must_change_password=true` para forzar rotación inmediata al ingresar al Backoffice.

---

## Relacionado

- [Operación Diaria del Backoffice SaaS](./operacion_backoffice_saas.md)
- [Manuales para Dueños y Personal de Restaurante](../restaurante/README.md)
- [Flujo de Alta de Empresa y Onboarding (Procesos de Negocio)](../../business-processes/04-administracion-y-personal/02_flujo_alta_empresa_y_onboarding.md)
- [Sub-Bóveda Técnica del Backoffice](../../backoffice/README.md)
- [Arquitectura Docker, Respaldos y 4 Ambientes](../../../docker/README.md)
- [Inicio del Repositorio](../../../README.md)
