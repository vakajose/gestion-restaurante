---
title: "ADR-0028: Motor Agnóstico de Almacenamiento (core.storage) y Tabla platform_settings"
category: "adr"
tags:
  - adr
  - storage
  - media
  - s3
  - platform-settings
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0028: Motor Agnóstico de Almacenamiento (`core.storage`) y Tabla `platform_settings`

- **Sprint / Tarea:** Sprint 6 — `TASK-603` (`PB-035`)

---

## 1. Contexto

El servicio `LocalFileStorageService` vivía acoplado dentro de `modules.catalog.internal`, escribía exclusivamente en disco local (`./uploads/dishes/`), aplicaba un límite estático (`MAX_FILE_SIZE = 1048576L`) y solo validaba la extensión del nombre de archivo sin inspeccionar *magic bytes*.

En producción sobre Oracle Cloud (OCI), los contenedores deben ser *stateless* respecto al disco efímero, usando **OCI Object Storage** (vía su API oficial de compatibilidad S3) y manteniendo almacenamiento local únicamente para `dev` o despliegues autónomos. Además, el administrador de plataforma requiere ajustar dinámicamente desde el Backoffice el proveedor activo y el límite máximo de subida (`1 MB` por defecto).

---

## 2. Decisión

### 2.1 Tabla `platform_settings` Separada de Tenants
Conforme a `ADR-0027`, las configuraciones globales de plataforma residen en **`platform_settings` (`V18`)** con llave foránea hacia `platform_admins(id)`, manteniendo `system_parameter_values` exclusivamente para parámetros de restaurantes (`TENANT`/`BRANCH`):
- `storage.active_provider` (`LOCAL` | `S3`, default `LOCAL`)
- `storage.max_file_size_bytes` (`NUMBER`, default `1048576` = `1 MB`, configurable en rango `102400`..`10485760`)
- `storage.public_url_prefix` (`STRING`, default `/uploads/dishes/` en local o base URL CDN/bucket en S3)

### 2.2 Patrón Strategy en `core.storage` + AWS SDK v2 para OCI
- **Módulo transversal `core.storage`:** Expone `MediaStorageService` (`core.storage.api`) consumido por `modules.catalog` y otros módulos sin dependencias circulares.
- **Estrategias intercambiables (`StorageStrategy`):**
  - `LocalStorageStrategy`: Persiste en `${app.storage.local-path}` manteniendo URLs `/uploads/dishes/...`.
  - `S3CompatibleStorageStrategy`: Usa **`software.amazon.awssdk:s3`** + **`url-connection-client`** (sin Netty/Apache HTTP, óptimo para Java 25 Virtual Threads) configurado con `forcePathStyle(true)` y `endpointOverride` compatible con **OCI Object Storage** (`https://<namespace>.compat.objectstorage.<region>.oraclecloud.com`), MinIO (`testing-local`), Cloudflare R2 y AWS S3. Las claves se aíslan por restaurante: `tenants/{tenantId}/dishes/dish_{dishId}_{rand}.{ext}`.
- **Aislamiento de Buckets:** El bucket de catálogo (lectura pública/CDN de imágenes de menú) es independiente del bucket privado destinado a respaldos de PostgreSQL (`TASK-606`).

### 2.3 Seguridad, Validación Binaria y Techo Multipart
- **Sin librerías Java de compresión WebP:** La optimización/recorte WebP 4:3 permanece en el frontend (`TASK-506`). El servidor valida extensión, MIME y cabecera binaria real (*magic bytes*: `RIFF....WEBP`, PNG `\x89PNG\r\n\x1a\n`, JPEG `\xFF\xD8\xFF`).
- **Techo Tomcat vs Límite Dinámico:** `application.yml` eleva `spring.servlet.multipart.max-file-size` a `10MB` (`max-request-size: 12MB`) para que cambios en caliente de `storage.max_file_size_bytes` desde Backoffice surtan efecto sin reiniciar la JVM.
- **Limpieza al Reemplazar:** Al subir una nueva foto de plato, se elimina la anterior de forma *best-effort* para evitar objetos huérfanos.

---

## 3. Consecuencias

- **Portabilidad y Simplicidad:** El mismo binario backend opera contra disco local en `dev`, MinIO S3 en `testing-local` y OCI Object Storage en `staging`/`prod`.
- **Seguridad:** Archivos ejecutables o scripts disfrazados con extensión `.webp` son rechazados en el análisis de *magic bytes*.

## Documentos Relacionados
- [ADR-0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica](./0002_hybrid_tenancy_and_dynamic_settings.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0021: Catálogo Visual y Recorte Nativo WebP](./0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)
- [ADR-0026: Estrategia de Infraestructura Cloud y Despliegue](./0026_cloud_infrastructure_and_deployment_stack.md)
- [ADR-0027: Identidad de Plataforma y Bootstrap Limpio](./0027_platform_identity_and_clean_golive_bootstrap.md)
- [ADR-0029: Motor SaaS de Backoffice, Cuotas y Telemetría](./0029_backoffice_subscriptions_quotas_and_telemetry.md)
- [ADR-0033: Configuración de Runtime Env en SPAs y Media Cross-Origin](./0033_spa_runtime_env_and_cross_origin_media.md)
- [Casos de Uso de Backoffice SaaS](../backoffice/architecture/use_cases_backoffice.md)
- [Casos de Uso de Catálogo](../project/architecture/03-use-cases/use_cases_catalog.md)
- [Casos de Uso de Configuración](../project/architecture/03-use-cases/use_cases_configuration.md)
