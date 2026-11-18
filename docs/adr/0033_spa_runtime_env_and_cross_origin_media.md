---
title: "ADR-0033: Configuración de Runtime Env en SPAs y Resolución de Media Cross-Origin"
category: "adr"
tags:
  - adr
  - devops
  - frontend
  - backoffice
  - media
  - security
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0033: Configuración de Runtime Env en SPAs y Resolución de Media Cross-Origin

## Contexto y Problema
El despliegue unificado de la plataforma ([ADR-0026](./0026_cloud_infrastructure_and_deployment_stack.md)) publica imágenes Docker inmutables en GHCR que promueven los mismos artefactos desde Testing Local hacia Staging en nube y Producción. Al compilar las SPAs (Angular ERP en `frontend/` y React Backoffice en `backoffice-frontend/`) con URLs hardcodeadas en tiempo de build (`environment.prod.ts` o variables `VITE_*` fijas), cada ambiente requería recompilar imágenes o fallaba al separarse los orígenes DNS (por ejemplo, `app.localhost` o `app.dominio.com` consumiendo `api.localhost` o `api.dominio.com`).

Adicionalmente, al almacenar imágenes de platos localmente o vía almacenamiento abstracto ([ADR-0028](./0028_agnostic_media_storage_and_platform_settings.md)) y devolver rutas relativas `/uploads/...` desde `WebConfig` y `MediaStorageService`, los navegadores en arquitectura split-origin resolvían `<img src="/uploads/...">` contra el origen del frontend (`app.*`) en lugar del backend (`api.*`), provocando imágenes rotas salvo proxy específico.

## Decisión
Se adopta el patrón **Build Once, Configure at Runtime** junto con resolución explícita de URLs de media en cliente y soporte redundante en reverse proxy:

1. **Inyección dinámica de `window.__ENV__` en arranque de contenedor:**
   - Tanto `frontend/docker/40-generate-env.sh` como `backoffice-frontend/docker/40-generate-env.sh` generan al iniciar el contenedor NGINX el archivo `/tmp/env.js` asignando `window.__ENV__ = { API_BASE_URL: "..." }`.
   - `nginx.conf` expone `/env.js` con cabecera `Cache-Control: no-store` para evitar que el navegador cachee configuraciones de otro ambiente, y ambos `index.html` cargan `<script src="env.js">` antes del bundle principal.
   - En Angular (`frontend/src/app/core/config/runtime-env.ts`), `getRuntimeApiBaseUrl()` lee `window.__ENV__.API_BASE_URL` y `apiBaseUrlInterceptor` reescribe peticiones relativas `/api/...` hacia el origen configurado. En Backoffice React (`backoffice-frontend/src/config/runtimeEnv.ts`), `getApiBaseUrl()` prioriza `window.__ENV__.API_BASE_URL` con fallback a `VITE_API_BASE_URL` en desarrollo local.
2. **Resolución determinista de media cross-origin (`resolveMediaUrl()`):**
   - `resolveMediaUrl()` en `frontend/src/app/core/config/runtime-env.ts` inspecciona la URL recibida de la API:
     - Mantiene intactas las URLs absolutas (`http://`, `https://`, compatibles con `S3CompatibleStorageStrategy`) así como esquemas `data:` y `blob:` (previsualización local del recortador de imagen de [ADR-0021](./0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)).
     - Para rutas relativas iniciadas en `/uploads/...`, antepone la base `API_BASE_URL` cuando el frontend y el backend operan en orígenes distintos.
3. **Cache inmutable y respaldo en Reverse Proxy Caddy:**
   - En backend, `WebConfig.java` sirve `/uploads/**` con caché HTTP de 365 días cuando se usa `LocalStorageStrategy`.
   - Tanto `docker/caddy/Caddyfile.testing-local` como `docker/caddy/Caddyfile.cloud` incorporan manejo explícito para `/uploads/*` hacia `backend:8080` en el dominio de la aplicación como capa redundante ante eventuales rutas relativas servidas desde caché antiguo.

## Consecuencias

### Positivas
- **Inmutabilidad real de artefactos Docker:** La misma imagen validada en `testing-local` o `staging` puede desplegarse en `production` cambiando únicamente variables de entorno del contenedor.
- **Compatibilidad híbrida Local vs S3:** `resolveMediaUrl()` funciona transparentemente tanto con almacenamiento local `/uploads/...` como con URLs públicas/externas emitidas por `S3CompatibleStorageStrategy` ([ADR-0028](./0028_agnostic_media_storage_and_platform_settings.md)).
- **Prevención de caché obsoleto de configuración:** `env.js` servido con `no-store` evita fallos tras migraciones de dominio o subdominio.

### Negativas y Mitigaciones
- **Dependencia de carga temprana de `env.js`:** Si un proxy o CDN alterara la ruta de `/env.js`, las SPAs caerían en fallback relativo al mismo origen. *Mitigación:* `nginx.conf` sirve `env.js` con ruta absoluta raíz y `Cache-Control: no-store`, verificado en los smoke checks de arranque de stack.

## Referencias de Implementación
- Frontend Angular: `frontend/public/env.js`, `frontend/src/index.html`, `frontend/docker/40-generate-env.sh`, `frontend/docker/nginx.conf`, `frontend/src/app/core/config/runtime-env.ts`, `frontend/src/app/core/interceptors/api-base-url.interceptor.ts`.
- Backoffice React: `backoffice-frontend/public/env.js`, `backoffice-frontend/index.html`, `backoffice-frontend/docker/40-generate-env.sh`, `backoffice-frontend/src/config/runtimeEnv.ts`.
- Backend & Proxy: `backend/src/main/java/com/restaurant/app/core/common/WebConfig.java`, `docker/caddy/Caddyfile.testing-local`, `docker/caddy/Caddyfile.cloud`.

## Documentos Relacionados
- [ADR-0021: Catálogo Visual, Recorte de Imágenes y Modos de Vista en POS](./0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)
- [ADR-0026: Stack de Infraestructura Cloud y Despliegue (Docker, Caddy, GHCR, OCI)](./0026_cloud_infrastructure_and_deployment_stack.md)
- [ADR-0028: Almacenamiento Agnóstico de Media y Parámetros de Plataforma](./0028_agnostic_media_storage_and_platform_settings.md)
- [Manual de Bootstrap y Operación Inicial](../manuals/plataforma/bootstrap_y_operacion_inicial.md)
- [Casos de Uso de Backoffice SaaS](../backoffice/architecture/use_cases_backoffice.md)
- [Casos de Uso de Catálogo](../project/architecture/03-use-cases/use_cases_catalog.md)
