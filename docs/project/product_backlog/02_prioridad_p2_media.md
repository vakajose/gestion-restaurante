---
title: "Product Backlog: Prioridad P2 (Media Prioridad)"
category: "product-backlog"
priority: "P2"
tags:
  - backlog
  - p2
  - medium-priority
  - architecture
  - hardware
  - ux
last_updated: "2026-11-19"
status: "active"
---

# 🟡 Product Backlog — Prioridad P2 (Media Prioridad)

> [!NOTE]
> **Definición de Prioridad P2:** Mejoras arquitectónicas no bloqueantes, integraciones de hardware (impresión directa), estándares fiscales, refactorizaciones conceptuales y optimizaciones de experiencia de usuario.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P2

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-006** | Desacoplamiento y Compatibilidad Multi-Motor SQL | Arquitectura de Datos | En Backlog | Por Planificar |
| **PB-007** | Facturación Electrónica Oficial | Integraciones Fiscales | En Backlog | Por Planificar |
| **PB-008** | Módulo de Impresión Física ESC/POS (Comanderas de Red) | Hardware / POS | En Backlog | Por Planificar |
| **PB-013** | Evolución a Modelo de Dominio Rico en Entidades Transaccionales | Arquitectura de Dominio / DDD | En Backlog | Por Planificar |
| **PB-014** | Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex | Patrones de Diseño | En Backlog | Por Planificar |
| **PB-015** | Extracción de Generador de Secuencia Diaria de Comandas | Clean Code & Concurrencia | En Backlog | Por Planificar |
| **PB-036** | Catálogo Visual con Imágenes de Platos en POS y Toggle | Catálogo & POS / UI | Completado (Sprint 5) | `TASK-506`, `TASK-629` |
| **PB-040** | Tour Guiado Interactivo de Primera Experiencia (`Product Tour`) | Onboarding & UX | Parcial (Sprint 3 — Rediseño `PB-049`) | `TASK-339` |
| **PB-045** | Perfil de Usuario Autoservicio y Nombres Reales | Auth / UI/UX | Completado (Sprint 5) | `TASK-509` |
| **PB-048** | Asistente de Códigos Secuenciales y Emojis en Categorías | Catálogo / UI/UX | Completado (Sprint 5) | `TASK-507` |
| **PB-050** | Configuración Dinámica y Duplicación de Plantillas de Menú en Apertura de Turno | Catálogo & Turnos POS / UX | En Backlog (Retrospectiva S4) | Por Planificar |
| **PB-059** | Humanización y Etiquetas de Consola de Parámetros *(antes PB-047 P2)* | Configuración / UX | Completado (Sprint 5) | `TASK-510` |
| **PB-061** | Unificación de Elementos de Carta, Clasificadores por Categoría (`0..*`) y Materialización `FULL_CATALOG` | Catálogo & POS / Arquitectura | En Backlog (Versión Futura) | `TASK-637`, `TASK-638` |
| **PB-064** | Separación de Dominios (`app.` vs raíz) y Aislamiento de Reputación Email (Transaccional vs Marketing) | Infraestructura & Entregabilidad | En Backlog | Por Planificar |
| **PB-065** | SEO Técnico (`sitemap.xml`, `robots.txt`, Search Console) e Imagen Open Graph (`1200×630`) | Web Pública & Presencia Digital | En Backlog | Por Planificar |
| **PB-066** | Analítica de Producto para Embudo de Onboarding y Detección de Fricción | Analítica & UX / Producto | En Backlog | Por Planificar |
| **PB-067** | Gobernanza de Cookies, Banner Simétrico (Aceptar/Rechazar) y Carga Diferida de Analítica | Privacidad & Compliance (RGPD/AEPD) | En Backlog | Por Planificar |

---

## Detalle Exhaustivo de Requerimientos P2

<a id="pb-006"></a>
### PB-006 [CORE] [ARQUITECTURA]: Desacoplamiento y Compatibilidad Multi-Motor SQL
- **Categoría:** Arquitectura de Datos
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Abstracción de dialectos relacionales en consultas JPA manteniendo estándar ANSI SQL para garantizar portabilidad futura.

---

<a id="pb-007"></a>
### PB-007 [POS] [NEGOCIO]: Facturación Electrónica Oficial
- **Categoría:** Integraciones Fiscales
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Emisión de facturas electrónicas oficiales con firma digital, QR tributario y contingencia offline ante servicios de impuestos nacionales.

---

<a id="pb-008"></a>
### PB-008 [POS] [INFRA]: Módulo de Impresión Física ESC/POS (Comanderas de Red)
- **Categoría:** Hardware / POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Envío directo de tickets vía TCP/IP a impresoras térmicas ESC/POS distribuidas por estaciones operativas (Cocina, Barra, Caja).

---

<a id="pb-013"></a>
### PB-013 [CORE] [ARQUITECTURA]: Evolución a Modelo de Dominio Rico (`Rich Domain Model`) en Entidades Transaccionales
- **Categoría:** Arquitectura de Dominio / DDD
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Encapsulamiento de lógica transaccional dentro de `Order` y `CashShift`, evitando modelos anémicos y dispersión de reglas en servicios.

---

<a id="pb-014"></a>
### PB-014 [INVENTORY] [ARQUITECTURA]: Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex
- **Categoría:** Patrones de Diseño / Módulo Inventory
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Inyección dinámica de estrategias mediante `Map<InventoryDeductionMode, DeductionStrategy>`, garantizando cumplimiento del Principio Abierto/Cerrado (OCP).

---

<a id="pb-015"></a>
### PB-015 [POS] [ARQUITECTURA]: Extracción de Generador de Secuencia Diaria de Comandas (`SRP` en POS)
- **Categoría:** Clean Code & Concurrencia / Módulo POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Delegación de correlativos diarios (`T-1001`) a un servicio atómico especializado seguro ante alta concurrencia.

---

<a id="pb-036"></a>
### PB-036 [CATALOG] [UI/UX]: Catálogo Visual con Imágenes de Platos en Punto de Venta y Toggle Parametrizable
- **Categoría:** Catálogo & POS / UI Operativa
- **Prioridad:** `P2 - Media`
- **Estado:** `Completado (Sprint 5 — TASK-506 / ADR-0021; proxy cross-origin resuelto en TASK-629)`
- **Contexto:** Habilitar fotografías optimizadas (WebP 4:3 `400x300` recortadas en navegador con `<app-image-cropper-modal>`) en Catálogo y cuadrícula POS mediante `core.storage` (`PB-035`), con selector triple de vistas (`COMPLETO`, `COMPACTO`, `LISTA`) y fallback a emoji de categoría.
- **Criterios de Aceptación:**
  - El cajero alterna vistas en POS y las imágenes cargan de forma segura en todos los ambientes (`dev`, `testing-local`, `staging`, `prod`).

---

<a id="pb-040"></a>
### PB-040 [UI/UX] [NEGOCIO]: Tour Guiado Interactivo de Primera Experiencia (`Product Tour Onboarding`)
- **Categoría:** Onboarding & UX / Primera Experiencia de Usuario
- **Prioridad:** `P2 - Media`
- **Estado:** `Parcial (Base funcional en Sprint 3 — TASK-339; rediseño integral de transiciones y cierre registrado en PB-049)`
- **Contexto:** Integración inicial de tour guiado tras onboarding (`TASK-339`); pendiente evolución hacia la experiencia simplificada de micro-metas de negocio definida en [`PB-049`](./01_prioridad_p1_alta.md#pb-049).

---

<a id="pb-045"></a>
### PB-045 [AUTH] [UI/UX]: Perfil de Usuario Autoservicio, Visualización de Nombres Reales y Limpieza de Privilegios Técnicos en UI
- **Categoría:** Autenticación & Seguridad / Perfil de Usuario
- **Prioridad:** `P2 - Media`
- **Estado:** `Completado (Sprint 5 — TASK-509 / ADR-0024)`
- **Contexto:** Despliegue de `fullName` y título legible de rol en sidebar/header/tabla `/users`, edición de perfil propio (incluyendo `phoneNumber` y PIN de supervisor de 4–6 dígitos) y eliminación de listas crudas de códigos de permiso en `/profile`.

---

<a id="pb-048"></a>
### PB-048 [CATALOG] [UI/UX]: Asistente de Codificación Secuencial de Platos y Selector de Iconos/Emojis para Categorías
- **Categoría:** Catálogo de Platos & Categorías / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `Completado (Sprint 5 — TASK-507 / ADR-0022)`
- **Contexto:** Columna `code_prefix` (`V15`), sugerencia automática de código SKU consecutivo (`GET /api/v1/catalog/dishes/next-code`), selector nativo `<app-emoji-picker>` y autocalculación de `sortOrder`.

---

<a id="pb-050"></a>
### PB-050 [CATALOG/POS] [UI/UX]: Configuración Dinámica, Edición y Duplicación de Plantillas de Menú en Apertura de Turno
- **Categoría:** Catálogo de Platos & Operación POS / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog (Retrospectiva Sprint 4)`
- **Contexto:** Permitir ajustar platos, activar/desactivar ítems y guardar variantes como nueva plantilla de menú directamente desde el flujo de apertura sin regresar a `/catalog/menus`.

---

<a id="pb-059"></a>
### PB-059 [CONFIG] [UI/UX]: Humanización Visual, Etiquetas Descriptivas y Rediseño de la Consola de Parametría Multinivel *(antes PB-047 P2)*
- **Categoría:** Configuración & Parámetros / Usabilidad
- **Prioridad:** `P2 - Media`
- **Estado:** `Completado (Sprint 5 — TASK-510 / ADR-0025)`
- **Nota de Numeración:** Renumerado de `PB-047` (P2) a `PB-059` para resolver colisión de identificador con [`PB-047`](./01_prioridad_p1_alta.md#pb-047) (P1 — Scripts de Reset/Semillas Dev).
- **Contexto:** Catálogo desacoplado `parameters-catalog.json` en español con títulos de negocio, descripciones de impacto, opciones humanizadas, popover `<app-context-help>` y buscador predictivo en `/settings/parameters`.

---

<a id="pb-061"></a>
### PB-061 [CATALOG/POS] [ARQUITECTURA]: Unificación de Elementos de Carta, Clasificadores por Categoría (`0..*`) y Materialización `FULL_CATALOG`
- **Categoría:** Catálogo & POS / Arquitectura Conceptual
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog (Versión Futura — TASK-637 y TASK-638)`
- **Contexto:** Evolucionar la arquitectura de combos dinámicos entregada en `TASK-634`:
  1. **Reemplazo de `CourseType` (`TASK-637`):** Sustituir `CourseType` (`STARTER`/`MAIN`/`DESSERT`/`BEVERAGE`/`GENERAL`) y `selectionSource = MENU_COURSE` para que los renglones dinámicos de combos referencien categorías reales del catálogo (`category_id`) con cardinalidad libre `0..*` (`min`/`max`) tanto en `DAILY_MENU` como en `FULL_CATALOG`.
  2. **Unificación Conceptual y Complementos en Combos (`TASK-638`):** Permitir asignar `ModifierGroup` a combos (además de platos), soportar recargos por selecciones adicionales `0..*` y evaluar materializar `FULL_CATALOG` como un menú completo de la carta para unificar filtros y combos.
- **Criterios de Aceptación:**
  - Sesiones de co-diseño SDD Fases 1 y 2 completadas (`Regla 8`) preservando retrocompatibilidad de datos históricos.

---

<a id="pb-064"></a>
### PB-064 [INFRA/DOMINIOS] [ARQUITECTURA]: Separación de Dominios (`app.` vs raíz) y Aislamiento de Reputación Email
- **Categoría:** Infraestructura & Entregabilidad SaaS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Fuente:** Sugerencias 1 y 2 del checklist pre-lanzamiento SaaS ([YouTube `DtcLLpZp-ys`](https://www.youtube.com/watch?v=DtcLLpZp-ys)).
- **Contexto y Alcance:**
  1. **Desacoplamiento Web vs Producto:** Publicar la web comercial/landing en el dominio raíz (`tudominio.com`) y alojar la aplicación operativa en subdominio dedicado (`app.tudominio.com` para ERP Restaurante y subdominio separado para Backoffice SaaS). Así, cualquier rediseño o despliegue en la web comercial no afecta ni rompe el producto.
  2. **Aislamiento de Reputación de Correos:** Separar subdominios de envío (SPF/DKIM/DMARC independientes) para correos transaccionales críticos de la app (recuperación de contraseña, invitaciones, alertas) frente a correos de marketing/newsletters, evitando que una campaña marcada como spam degrade la entregabilidad transaccional. Para prospección en frío a clientes, utilizar un dominio aparte.
- **Criterios de Aceptación:**
  - Arquitectura DNS/Nginx/Caddy con rutas separadas entre web comercial (`tudominio.com`) y aplicación (`app.tudominio.com`).
  - Registros SPF, DKIM y DMARC configurados por subdominio separado para correo transaccional vs marketing.

---

<a id="pb-065"></a>
### PB-065 [WEB/SEO] [MARKETING]: SEO Técnico (`sitemap.xml`, `robots.txt`, Search Console) e Imagen Open Graph (`1200×630`)
- **Categoría:** Web Pública & Presencia Digital
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Fuente:** Sugerencias 3, 4 y 5 del checklist pre-lanzamiento SaaS ([YouTube `DtcLLpZp-ys`](https://www.youtube.com/watch?v=DtcLLpZp-ys)).
- **Contexto y Alcance:**
  1. **`sitemap.xml` + Google Search Console:** Generar `sitemap.xml` con todas las páginas públicas indexables y registrarlo en Google Search Console para acelerar la indexación orgánica.
  2. **`robots.txt`:** Publicar `robots.txt` con la directiva `Sitemap: https://.../sitemap.xml` apuntando al sitemap público, y restringir explícitamente rutas privadas de la aplicación operativa si comparten origen o exposición.
  3. **Open Graph (`1200×630`):** Diseñar e integrar una imagen `og:image` de `1200×630` px y metaetiquetas Open Graph / Twitter Cards en la web pública y en `index.html` de las SPAs, evitando que al compartir enlaces aparezca un recuadro gris genérico.
- **Criterios de Aceptación:**
  - `sitemap.xml` y `robots.txt` accesibles vía HTTP 200 y validados en Google Search Console.
  - Previsualización correcta de tarjeta social (`1200×630`) al compartir URLs en WhatsApp, Slack, LinkedIn y X.

---

<a id="pb-066"></a>
### PB-066 [ANALYTICS/UX] [BI]: Analítica de Producto para Embudo de Onboarding y Detección de Fricción
- **Categoría:** Analítica & UX / Producto
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Fuente:** Sugerencia 6 del checklist pre-lanzamiento SaaS ([YouTube `DtcLLpZp-ys`](https://www.youtube.com/watch?v=DtcLLpZp-ys)).
- **Contexto y Alcance:**
  - Instrumentar analítica de producto (orientada a eventos de negocio, sin exponer PII innecesaria) para identificar dónde se atascan los primeros restaurantes/usuarios durante el onboarding y operación inicial (ej. registro, creación de catálogo, apertura de primer turno, primera venta POS).
  - Evaluar opción *privacy-first / cookieless* (ej. Plausible, Umami o PostHog sin cookies) vs analítica con almacenamiento local (que requeriría consentimiento explícito bajo `PB-067`).
- **Criterios de Aceptación:**
  - Embudo de eventos clave instrumentado desde el onboarding hasta la primera venta completada en POS.
  - Tablero operativo que permita detectar tasas de abandono por paso.

---

<a id="pb-067"></a>
### PB-067 [PRIVACY/COMPLIANCE] [SEGURIDAD/UX]: Gobernanza de Cookies, Banner Simétrico (Aceptar/Rechazar) y Carga Diferida de Analítica
- **Categoría:** Privacidad & Compliance (RGPD / AEPD)
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Fuente:** Sugerencia 7 del checklist pre-lanzamiento SaaS ([YouTube `DtcLLpZp-ys`](https://www.youtube.com/watch?v=DtcLLpZp-ys)).
- **Contexto y Alcance:**
  - Garantizar cumplimiento estricto de normativa de privacidad (RGPD / criterios AEPD, evitando multas por consentimiento inválido o *dark patterns*):
    1. Si la analítica o cualquier script de terceros guarda cookies/identificadores en el navegador, **no debe cargar ni ejecutarse hasta que el usuario acepte explícitamente**.
    2. El banner de cookies debe ofrecer botones de **Aceptar** y **Rechazar** con la misma jerarquía visual y facilidad de acceso.
    3. Si se opta por analítica 100% *cookieless* que no almacena nada en el navegador, documentar la exención técnica de banner.
- **Criterios de Aceptación:**
  - Verificación en navegador modo incógnito (DevTools → Application → Cookies / LocalStorage de terceros): cero cookies de analítica/tracking presentes antes de hacer clic en "Aceptar".
  - Botón de rechazo funcional en el primer nivel del banner, con bloqueo efectivo de scripts no esenciales al rechazar.
