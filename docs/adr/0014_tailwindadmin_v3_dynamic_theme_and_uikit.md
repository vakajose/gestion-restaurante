# ADR-0014: Sistema de Diseño TailwindAdmin v3.0, Motor de Temas Dinámico en Caliente y Suite de Componentes Standalone (UI Kit)

* **Estado:** Aceptado
* **Fecha:** 2026-10-29
* **Autor:** Arquitecto de Software / Squad Frontend & Documentación (`TASK-404`)
* **Relacionado con:** `PB-042`, `PB-043`, `OBS-UI-01` a `OBS-UI-10`, [ADR-0012](0012_mobile_first_and_touch_ergonomics.md), [05_design_system.md](../sdd/05_design_system.md)

---

## 1. Contexto y Problema

Durante la retrospectiva de cierre del Sprint 3 ([`retrospectiva_sprint3.md`](../project/retrospectiva_sprint3.md)), se identificaron diez deficiencias estructurales de interfaz y experiencia de usuario (`OBS-UI-01` a `OBS-UI-10`) originadas por una adopción parcial e inconsistente de la plantilla de referencia `TailwindAdmin`:

1. **Ausencia de Jerarquía de Superficies en Modo Claro (`OBS-UI-01`):** El canvas de fondo (`body` / `main`), el menú lateral (`aside`), la barra superior (`header`) y las tarjetas (`.card`) utilizaban blanco puro (`#ffffff`) o un gris casi imperceptible (`#f6f9fc`, 98% luminosidad) con bordes muy tenues (`#eaeff4`), provocando fatiga visual y pérdida de delimitación espacial.
2. **Menú Lateral Rígido en Desktop y Tablet (`OBS-UI-02`):** El `<aside>` (`270px`) carecía de control de colapso en resoluciones `>= 1024px` (`lg:static lg:translate-x-0` forzado y botón hamburguesa oculto mediante `lg:hidden`), impidiendo ganar espacio horizontal en pantallas de punto de venta o tablas densas.
3. **Colisión de Contextos de Apilamiento (`z-index`) en el Topbar (`OBS-UI-03`):** El `<header>` aplicaba `backdrop-blur` sin un `z-index` explícito superior al de `<main>`, creando un *stacking context* que atrapaba al menú desplegable del selector de sucursal (`z-50` local) por debajo de las tarjetas con `position: relative` del área de contenido.
4. **Márgenes de Página Inconsistentes (`OBS-UI-04`):** El elemento `<main>` no aplicaba un contenedor `.pageWrapper` con padding canónico (`24px`), provocando que vistas como Catálogo, Finanzas, Inventario y POS quedaran pegadas a los bordes (`0px`), mientras que otros módulos definían `p-6` manualmente.
5. **Falta de Semántica en Botones, Badges y Controles de Formulario (`OBS-UI-05`, `OBS-UI-06`, `OBS-UI-07`, `OBS-UI-09`):**
   - Botones de "Cancelar" y "Aceptar" compartían estilos similares en distintos modales.
   - Badges en modo claro utilizaban colores de texto brillantes (`#13deb9`, `#ffae1f`) sobre fondos pastel pálidos, incumpliendo el ratio mínimo 4.5:1 de WCAG 2.2 AA.
   - Los controles de formulario carecían de un wrapper estandarizado que aplicara borde de error (`border-error`), mensaje de validación inline debajo del input y *auto-scroll* al primer campo inválido en modales extensos.
6. **Alertas Ocultas Detrás de Modales Activos (`OBS-UI-08`):** Los errores de negocio devueltos por el backend (`RFC 7807 ProblemDetail`) se renderizaban como alertas en la vista padre, quedando ocultos detrás del backdrop (`z-50`) cuando la acción se originaba desde un modal abierto.

---

## 2. Decisiones Arquitectónicas

### 2.1. Motor de Temas Dinámico en Caliente mediante CSS Custom Properties (Zero-Reload)
Se decide desacoplar los colores físicos (Hex/OKLCH) de los componentes Angular mediante **Variables CSS nativas (`:root` / atributos `data-*` en `<html>`)** enlazadas a los tokens semánticos de Tailwind CSS (`bg-canvas`, `bg-surface`, `bg-sidebar`, `bg-topbar`, `border-border`, `bg-primary`, etc.).

El servicio reactivo `ThemeService` gestiona cuatro ejes de personalización en tiempo real (`0ms` de repintado sin recargar la página), persistidos en `localStorage` (`app_ui_preferences`) con soporte de valor predeterminado por empresa (`UI_THEME_DEFAULT`):

1. **Modo de Luminosidad (`themeMode`):** `'light' | 'dark'`.
2. **Preset de Contraste de Superficie (`surfacePreset`):**
   - **`slate-clean` (Predeterminado):** Canvas en gris pizarra suave (`#f1f5f9`), tarjetas/sidebar/topbar en blanco puro (`#ffffff`), delimitados por bordes nítidos de `1px` (`#dce3ec`) y sombra multicapa `--theme-shadow-md`.
   - **`warm-tinted` (Sidebar Tinteado):** Canvas en `#f3f5f8`, Sidebar diferenciado en tono pizarra claro (`#f8fafc`) y tarjetas en blanco puro (`#ffffff`) con borde `#e2e8f0`.
   - **`dark-nav` (Sidebar Oscuro ERP):** Sidebar fijo en azul noche (`#1e293b` con texto `#cbd5e1` incluso en modo claro), Canvas en `#f1f5f9` y tarjetas en `#ffffff`.
3. **Paleta de Color de Marca (`brandPalette`):** Inspirada en los temas oficiales de `TailwindAdmin` (`blue` `#5d87ff`, `aqua` `#0074ba`, `purple` `#763ebd`, `emerald` `#0a7ea4`, `orange` `#fa896b`).
4. **Modo de Colapso del Sidebar en Desktop (`sidebarCollapseMode`):**
   - **`mini-rail` (Predeterminado):** En pantallas `>= 1024px`, el botón hamburguesa contrae el menú lateral de `270px` a un riel compacto de **`76px`** (mostrando únicamente los íconos centrados con tooltip y ocultando etiquetas de texto y encabezados de sección).
   - **`full-hide`:** En pantallas `>= 1024px`, el botón hamburguesa oculta completamente el menú lateral (`0px`), cediendo el 100% del ancho útil al área de trabajo `<main>`.

### 2.2. Estrategia Híbrida de Iconografía Offline-First (`Solar` + `Tabler` + Emojis)
- Se adoptan los vectores SVG oficiales de **Solar Icons** (licencia abierta **CC BY 4.0**) y **Tabler Icons** (licencia abierta **MIT**), idénticos a los empleados por `TailwindAdmin`.
- **Prohibición de carga por CDN en tiempo de ejecución:** Para preservar la arquitectura **Offline-First** del POS (ADR-0003), se descarta el uso del script externo `iconify-icon.min.js` que consulta `api.iconify.design` por red. En su lugar, el componente `<app-icon>` empaqueta localmente un registro tipado con los `<path>` vectoriales oficiales de Solar/Tabler.
- **Convivencia con Emojis:** Los íconos vectoriales `<app-icon>` son obligatorios en el Shell (Sidebar, Topbar, selector de temas, modales, toasts y controles del UI Kit), mientras que los emojis siguen permitidos en etiquetas de contenido semántico o gastronómico donde aporten expresividad visual inmediata.

### 2.3. Escala Canónica de Capas (`z-index`)
Para erradicar colisiones visuales entre menús desplegables, barras pegajosas, cajones móviles, modales y notificaciones, se establece la siguiente jerarquía estricta:

| Capa | Clase Tailwind | Valor `z-index` | Elementos Asignados |
| :--- | :--- | :---: | :--- |
| **Nivel 0: Base / Flujo** | `z-0` / `z-10` | `0` – `10` | Tarjetas (`.card`), tablas y contenido dentro de `<main>`. |
| **Nivel 1: Cabecera Fija** | `z-30` | `30` | `<header>` principal (Topbar sticky con `backdrop-blur`). |
| **Nivel 2: Navegación y Popovers** | `z-40` / `z-50` | `40` – `50` | Backdrop móvil (`z-40`), Sidebar Drawer móvil (`z-50`), Dropdown de Sucursales (`z-50`) y `<app-theme-customizer>` (`z-50`). |
| **Nivel 3: Modales de Negocio** | `z-[100]` | `100` | Diálogos `<app-modal>` y `<app-bottom-sheet>`. |
| **Nivel 4: Modales Críticos de Sistema** | `z-[200]` | `200` | `<app-inactivity-modal>` (bloqueo de seguridad) y `<app-product-tour>`. |
| **Nivel 5: Portal Global de Toasts** | `z-[9999]` | `9999` | `<app-toast-portal>` (notificaciones y errores `RFC 7807 ProblemDetail`, siempre visibles sobre cualquier modal). |

### 2.4. Suite de 8 Componentes Standalone del UI Kit (`src/app/shared/components/ui/`)
Se estandariza una biblioteca interna de componentes Angular 22 Standalone (Zoneless, `input()` Signals) en `frontend/src/app/shared/components/ui/`:
1. **`<app-icon>`:** Renderizador SVG vectorial local Solar/Tabler.
2. **`<app-page-wrapper>`:** Contenedor canónico de vista (`max-w-[1536px] mx-auto` o `fluid`, cabecera homogénea y slot `[pageActions]`).
3. **`<app-button>` / `button[app-button]`:** Botón semántico con distinción estricta entre acción afirmativa (`primary`), cancelación neutra (`neutral`), peligro (`danger`), variantes suaves (`soft-*`, `outline-*`, `ghost`) y estado `[loading]`.
4. **`<app-badge>`:** Insignia de estado con paleta de texto oscurecida en modo claro y luminosa en modo oscuro para garantizar ratio $\ge 4.5:1$ (WCAG AA).
5. **`<app-card>`:** Contenedor elevado con superficie `bg-surface`, borde `border-border` y sombra multicapa `--theme-shadow-md`.
6. **`<app-modal>`:** Diálogo modal accesible (`z-[100]`) con resumen sticky de errores de validación y pie de acciones estandarizado.
7. **`<app-form-field>`:** Envoltura de campo de formulario con etiqueta, indicador de obligatoriedad, tinción automática de borde en rojo (`.has-error`) y mensaje de error inline debajo del control.
8. **`<app-toast-portal>` + `NotificationService`:** Sistema global de notificaciones flotantes en `z-[9999]` integrado con `errorInterceptor`.

---

## 3. Consecuencias

### Positivas
* **Cero Fatiga Visual y Personalización Instantánea:** El restaurante o cada operador puede elegir entre tres niveles de contraste de superficie y cinco paletas de marca con refresco en `0ms`.
* **Visibilidad Garantizada de Errores sobre Modales:** Ningún error `RFC 7807` vuelve a quedar oculto detrás de un modal gracias a la conexión entre `errorInterceptor`, `NotificationService` y `<app-toast-portal>` (`z-[9999]`).
* **Adopción Gradual sin Ruptura (Backward Compatible):** Las clases utilitarias existentes (`.card`, `.btn`, `.form-control`, `.badge-md`) heredan automáticamente las nuevas variables CSS de contraste, mientras los módulos de negocio migran progresivamente a los componentes `<app-*>`.

### Negativas / Trade-offs
* **Mantenimiento del Registro de Íconos Local:** Al no usar CDN externo por el requisito Offline-First, cuando se requiera un nuevo ícono de Solar/Tabler deberá agregarse su `<path>` oficial al diccionario `SOLAR_ICON_REGISTRY` de `<app-icon>`.
