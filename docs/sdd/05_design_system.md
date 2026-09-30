# 05. Sistema de Diseño - Especificación TailwindAdmin v2.0

## 1. Identidad Visual y Filosofía de Diseño
El sistema de diseño de la aplicación se fundamenta en la plantilla **TailwindAdmin**, ofreciendo una interfaz administrativa moderna, limpia, de alto contraste visual y pensada para la alta velocidad operativa requerida en terminales de Punto de Venta (POS) y paneles ERP de restaurantes.

### Principios Clave:
1. **Claridad sobre saturación:** Eliminación de gradientes de neón fluorescentes o fondos ruidosos. Fondos suaves y superficies sobrias.
2. **Elevación y Profundidad:** Sombras suaves multicapa (`--theme-shadow-md`) que separan claramente las tarjetas del canvas de fondo sin saturar.
3. **Escala Relativa y Accesibilidad:** Tipografía basada en unidades estándar (`rem`) sobre la fuente `DM Sans`, garantizando que el zoom nativo del navegador (`Ctrl +` / táctil) escale armónicamente textos y contenedores.
4. **Modo Oscuro de Alto Contraste:** Superficies oscuras en `#202938` y `#2a3547` con bordes `#333f55` y textos en blanco/gris claro (`#7c8fac`) cumpliendo WCAG AA.

---

## 2. Tipografía y Radios de Borde

### 2.1. Tipografía Base
- **Fuente Principal:** [DM Sans](https://fonts.google.com/specimen/DM+Sans) (`DM Sans, sans-serif`).
- **Pesos Utilizados:** Regular (400), Medium (500), SemiBold (600), Bold (700).
- **Jerarquía Tipográfica:**
  - `H1 - H6`: `font-semibold text-link dark:text-white` (Light: `#2a3547`, Dark: `#ffffff`).
  - `Body`: `text-sm text-bodytext dark:text-darklink` (Light: `#5a6a85`, Dark: `#7c8fac`).
  - `Card Title`: `text-lg font-semibold text-link dark:text-white`.
  - `Card Subtitle`: `text-sm text-bodytext dark:text-darklink`.
  - `Captions / Subencabezados`: `text-xs font-bold uppercase tracking-wider text-bodytext/70 dark:text-darklink/70`.

### 2.2. Radios de Borde (`Border Radius`)
- `rounded-md`: `7px` (predeterminado para cards, botones, inputs y paneles modales).
- `rounded-sm`: `4px` (insignias compactas y checkboxes).
- `rounded-full`: `9999px` (avatares circulares, pills de estado y conmutadores).

---

## 3. Paleta de Colores y Tokens Semánticos (OKLCH + Hex)

TailwindAdmin define una paleta cromática semántica en OKLCH nativo con equivalencias exactas en RGB/Hex:

| Token Semántico | Valor OKLCH | Valor Hex | Rol y Aplicación en la Interfaz |
| :--- | :--- | :--- | :--- |
| `primary` | `oklch(65.33% 0.184 266.79)` | `#5d87ff` | Color de marca, botones principales, enlaces activos, foco |
| `primary-emphasis` | `oklch(58.11% 0.161 266.76)` | `#4570ea` | Estado hover y active de elementos primarios |
| `lightprimary` | `oklch(65.33% 0.184 266.79 / 12.5%)` | `#ecf2ff` | Fondo del login, hover en menú lateral, badges primarios |
| `secondary` | `oklch(76.32% 0.139 237.20)` | `#49beff` | Acciones secundarias, badges de apoyo |
| `secondary-emphasis` | `oklch(67.79% 0.122 236.84)` | `#2ba8fb` | Hover secundario |
| `lightsecondary` | `oklch(76.32% 0.139 237.20 / 12.5%)` | `#e8f7ff` | Banners promocionales o informativos |
| `success` | `oklch(80.48% 0.150 174.63)` | `#13deb9` | Pagos confirmados, sincronizado con éxito, stock disponible |
| `lightsuccess` | `oklch(98.05% 0.027 182.42)` | `#e6fffa` | Fondo de alertas e insignias de éxito |
| `warning` | `oklch(80.94% 0.165 74.03)` | `#ffae1f` | Turnos pendientes, alertas de stock mínimo |
| `lightwarning` | `oklch(97.29% 0.023 82.12)` | `#fef5e5` | Fondo de advertencias |
| `error` | `oklch(74.75% 0.144 35.82)` | `#fa896b` | Alertas de error, pedidos cancelados, fallas de red |
| `lighterror` | `oklch(95.74% 0.019 38.15)` | `#fdede8` | Fondo de alertas RFC 7807 Problem Details |
| `info` | `oklch(68.98% 0.165 257.10)` | `#539bff` | Indicadores de contexto, notas informativas |
| `lightinfo` | `oklch(96.13% 0.017 256.28)` | `#ebf3fe` | Fondo de insignias informativas |
| `white` | `oklch(100% 0 0)` | `#ffffff` | Fondo de tarjetas y paneles en modo claro |
| `dark` | `oklch(27.84% 0.027 257.53)` | `#202938` / `#2a3547` | Superficie de fondo general y tarjetas en modo oscuro |
| `border` | `oklch(95.50% 0.009 242.84)` | `#eaeff4` | Bordes estructurales de tarjetas, tablas y encabezados |
| `bordergray` | `oklch(92.03% 0.015 260.73)` | `#dfe5ef` | Bordes de inputs y formularios |
| `darkborder` | `oklch(36.67% 0.041 262.29)` | `#333f55` | Bordes y divisores en modo oscuro |
| `link` | `oklch(32.70% 0.035 260.11)` | `#2a3547` | Color de texto principal en modo claro |
| `bodytext` | `oklch(52.16% 0.047 260.80)` | `#5a6a85` | Color de texto descriptivo y párrafos |
| `darklink` | `oklch(64.54% 0.049 258.74)` | `#7c8fac` | Color de texto secundario en modo oscuro |
| `lightgray` | `oklch(98.07% 0.005 247.88)` | `#f6f9fc` | Canvas de fondo general (dashboard) en modo claro |

---

## 4. Elevación y Sombras Multicapa

TailwindAdmin abandona las sombras duras de color negro sólido en favor de sombras multicapa con tinte azul pizarra:

* **Sombra General (`--theme-shadow-md`):**
  `rgba(145, 158, 171, 0.2) 0px 0px 2px 0px, rgba(145, 158, 171, 0.12) 0px 12px 24px -4px`
* **Sombra Modo Oscuro (`--theme-shadow-dark-md`):**
  `rgba(0, 0, 0, 0.3) 0px 0px 2px 0px, rgba(0, 0, 0, 0.25) 0px 12px 24px -4px`
* **Sombra Sutil (`--theme-shadow-sm`):**
  `0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)`

---

## 5. Anatomía de Componentes Estándar

### 5.1. Tarjetas (`Cards`)
- Clase utilitaria: `.card`
  - Estructura: `rounded-[7px] bg-white dark:bg-dark shadow-md dark:shadow-dark-md border border-border dark:border-darkborder relative w-full break-words`
- Contenido interior: `.card-body`
  - Espaciado: `p-6 sm:p-8`
- Títulos y subtítulos:
  - `.card-title`: `text-lg font-semibold text-link dark:text-white`
  - `.card-subtitle`: `text-sm text-bodytext dark:text-darklink mt-1`

### 5.2. Botones
- **Botón Primario (`.btn`):**
  - `rounded-[7px] bg-primary hover:bg-primaryemphasis text-white text-sm py-2.5 px-4 font-medium transition-colors cursor-pointer text-center inline-flex items-center justify-center gap-2`
- **Botón Secundario (`.btn-secondary`):**
  - `rounded-[7px] bg-secondary hover:bg-secondaryemphasis text-white text-sm py-2.5 px-4 font-medium transition-colors`
- **Botón Light Primary (`.btn-light-primary`):**
  - `rounded-[7px] bg-lightprimary text-primary hover:bg-primary hover:text-white dark:hover:bg-primary dark:hover:text-white text-sm py-2.5 px-4 font-medium transition-colors`
- **Botón Outline (`.btn-outline-primary`):**
  - `rounded-[7px] border border-primary text-primary hover:bg-primary hover:text-white bg-transparent text-sm py-2.5 px-4 font-medium transition-colors`

### 5.3. Controles de Formulario e Inputs
- **Input Estándar (`.form-control`):**
  - `rounded-[7px] border border-bordergray dark:border-darkborder bg-transparent w-full text-sm py-2.5 px-3.5 text-link dark:text-white placeholder-bodytext/60 focus:outline-none focus:border-primary transition-all`
- **Etiquetas (`Labels`):**
  - `text-xs font-semibold uppercase tracking-wider text-link dark:text-darklink mb-2 block`
- **Checkboxes y Radios:**
  - `w-4 h-4 rounded-[4px] border-bordergray dark:border-darkborder text-primary focus:ring-0 bg-transparent`

### 5.4. Insignias y Badges
- **Badge Básico (`.badge`):**
  - `inline-flex items-center gap-1.5 py-1 px-2.5 rounded-[4px] text-xs font-medium`
  - Éxito: `bg-lightsuccess text-success`
  - Primario: `bg-lightprimary text-primary`
  - Advertencia: `bg-lightwarning text-warning`
  - Error: `bg-lighterror text-error`

### 5.5. Barra Lateral (`Sidebar`)
- **Ancho:** `270px` fijo (`w-[270px]`).
- **Superficie:** `bg-white dark:bg-dark border-r border-border dark:border-darkborder h-screen flex flex-col`.
- **Item de Menú (`.sidebar-link`):**
  - `flex items-center text-sm font-normal py-2.5 px-3.5 mx-3 my-0.5 rounded-[7px] text-bodytext dark:text-darklink hover:bg-lightprimary hover:text-primary transition-colors gap-3`
- **Item Activo (`.activemenu`):**
  - `bg-primary text-white font-medium hover:bg-primary hover:text-white shadow-sm`
- **Sección (`.caption`):**
  - `text-xs font-bold uppercase tracking-wider text-bodytext/60 dark:text-darklink/60 px-4 pt-4 pb-2`

### 5.6. Barra Superior (`Topbar / Header`)
- **Altura:** `64px` (`h-16`).
- **Propiedades:** `sticky top-0 z-20 bg-white/90 dark:bg-dark/90 backdrop-blur border-b border-border dark:border-darkborder px-6 flex items-center justify-between`.
- **Elementos:** Selector de sucursal, insignias de estado del sistema, botón circular de cambio de tema y perfil de usuario con avatar.

### 5.7. Pantalla de Autenticación (Login / Register)
- **Fondo:** Canvas sobrio y suave `bg-lightprimary dark:bg-[#1b222c]` que cubre toda la pantalla (`min-h-screen`). Se descartan definitivamente los orbes difuminados de Datta Able.
- **Tarjeta Central:** `card max-w-[460px] w-full p-8 sm:p-10 shadow-md rounded-[7px] bg-white dark:bg-dark border border-border dark:border-darkborder`.
- **Composición:** Logo del restaurante centrado, formulario con `.form-control`, enlaces secundarios (`¿Olvidaste tu contraseña?`) y botón de submit destacado `.btn w-full`.

---

## 6. Persistencia y Gestión del Modo Oscuro

1. La clase `.dark` se manipula reactivamente en la etiqueta raíz `<html>`.
2. El servicio `ThemeService` de Angular expone:
   - `readonly isDarkMode = signal<boolean>(...)`
   - Sincronización automática mediante `effect()` con `localStorage.getItem('app_theme')`.
   - Soporte para detección automática del sistema (`window.matchMedia('(prefers-color-scheme: dark)')`).
3. El conmutador de tema se ubica tanto en el encabezado del login público como en el Topbar general de la aplicación.