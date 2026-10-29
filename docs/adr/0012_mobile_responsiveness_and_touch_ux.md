---
title: "ADR-0012: Adaptabilidad Móvil, Breakpoints Canónicos y Experiencia Táctil para POS e Inventario"
category: "adr"
status: "accepted"
date: "2026-10-28"
tags:
  - ui-ux
  - mobile-first
  - responsive-design
  - touch-ux
  - bottom-sheet
  - tailwindcss
  - pos
  - inventory
---

# ADR-0012: Adaptabilidad Móvil, Breakpoints Canónicos y Experiencia Táctil para POS e Inventario

## Contexto y Definición del Problema

El sistema Restaurante POS & ERP fue concebido inicialmente con un enfoque centrado en terminales de escritorio y pantallas de mostrador estándar ($\ge 1024\text{px}$). No obstante, la dinámica operativa real de los restaurantes modernos exige una versatilidad de dispositivos mucho más amplia:

1. **Atención Móvil en Mesa (Comanderos / Smartphones):** Los meseros y garzones toman pedidos directamente al lado del comensal utilizando teléfonos móviles corporativos o personales (Android / iOS). En la interfaz de escritorio actual, el catálogo y la comanda se organizan en dos columnas horizontales fijas (`lg:flex-row`). Al reducirse la pantalla a móviles (`< 768px`), la comanda se apila debajo del catálogo (`flex-col`), quedando oculta tras un scroll interminable de platos. El mesero no puede ver el total acumulado ni verificar los platos agregados sin desplazarse hasta el fondo de la pantalla.
2. **Conteo Físico en Bodegas y Almacenes de Insumos:** El personal de cocina o almacén realiza el levantamiento de inventario diario y semanal caminando entre estanterías con un smartphone. La vista de inventario actual presenta tablas relacionales densas de 7 y 8 columnas (`Insumo`, `Unidad`, `Stock Actual`, `Costo Promedio`, `Valorización`, `Estado`, `Acciones`). En una pantalla de 360px a 430px de ancho, estas tablas provocan un desbordamiento horizontal severo (*overflow-x*), obligando al usuario a realizar scroll horizontal constante para presionar los botones de ajuste o ver el saldo resultante.
3. **Ergonomía Táctil y Prevención de Errores Operativos:** Muchos botones e iconos de la interfaz de escritorio poseen dimensiones reducidas ($\le 32\text{px}$), lo que genera fallos de pulsación accidental con dedos en pantallas táctiles (violando las pautas de accesibilidad WCAG 2.2 que exigen objetivos táctiles mínimos de $44 \times 44\text{px}$).
4. **Entrada de Datos Numéricos en Pantalla Táctil:** Al abrir modales centrados para ajuste de stock o cobro en efectivo, el despliegue del teclado virtual nativo del smartphone oculta los campos de entrada o empuja el modal fuera del viewport visible.

---

## Opciones Evaluadas

### Opción 1: Desarrollo de Aplicaciones Nativas Dedicadas (Flutter / Android Kotlin / iOS)
- **Ventajas:** Máximo rendimiento nativo, control granular del hardware y gestos táctiles del sistema operativo.
- **Desventajas:** Duplicación masiva de código frontend, mayor costo de mantenimiento, doble ciclo de releases en tiendas (Google Play / App Store) y pérdida de la ventaja del stack unificado Angular 22 Zoneless con Dexie.js Offline First.

### Opción 2: Reducción Forzada de Tablas de Escritorio (Zoom-out y Scroll Horizontal Asistido)
- **Ventajas:** Rápido de implementar con CSS mínimo (`overflow-x-auto`, reducción de tamaño de fuente a 10px).
- **Desventajas:** Experiencia de usuario deficiente; números y textos ilegibles en pantallas pequeñas, fatiga visual y alta propensión a errores de digitación en turnos de alta demanda.

### Opción 3: Sistema Adaptativo Mobile-First con Componentes Táctiles Especializados (Opción Seleccionada)
- **Enfoque:** Arquitectura responsiva unificada en Angular 22 y Tailwind CSS, implementando patrones móviles modernos:
  - **POS:** Barra pegajosa inferior de comanda (*Floating Bottom Sheet Bar*) persistente que muestra cantidad y total, desplegando un panel táctil deslizable (*Swipe-up Drawer*) con el detalle de la orden.
  - **Catálogo Dual en Móvil:** Selector de vista entre *Cuadrícula Táctil* (tarjetas visuales de 2 columnas) y *Lista Rápida Compacta* (filas horizontales de un solo toque con botón "+" grande).
  - **Inventario:** Sustitución automática de la tabla de escritorio por *Tarjetas de Insumo Apilables con Acordeón Expandible* en pantallas `< 768px`.
  - **Modales en Bottom Sheet:** Transformación responsiva de diálogos centrados a hojas inferiores ancladas al fondo de la pantalla, adaptadas ergonómicamente a la zona de alcance del pulgar y conviviendo limpiamente con el teclado virtual.

---

## Decisión Arquitectónica y Especificación Técnica

Se adopta formalmente la **Opción 3: Sistema Adaptativo Mobile-First con Componentes Táctiles Especializados**.

### 1. Escala Canónica de Breakpoints de Interfaz

Se estandariza el uso estricto de la rejilla de breakpoints de Tailwind CSS para todo el frontend:

| Breakpoint | Rango de Ancho | Ámbito de Dispositivo | Comportamiento del Layout |
| :--- | :--- | :--- | :--- |
| **`xs` (Base)** | `< 640px` | Smartphones Portrait (360px – 430px) | Layout de 1 columna. Sidebar en drawer off-canvas. POS con Floating Bottom Sheet Bar. Inventario en tarjetas apilables acordeón. Targets táctiles $\ge 44\text{px}$. |
| **`sm`** | $640\text{px} \dots 767\text{px}$ | Smartphones Grandes / Phablets Landscape | Catálogo POS en 2 columnas. Tarjetas de inventario con datos financieros desplegados. |
| **`md`** | $768\text{px} \dots 1023\text{px}$ | Tablets Portrait (iPads, Android Tabs) | Sidebar en drawer colapsable con hamburguesa en Topbar. Catálogo en 3 columnas. Comanda en panel lateral o drawer expandible. |
| **`lg`** | $1024\text{px} \dots 1279\text{px}$ | Tablets Landscape / TPV Mostrador | Sidebar visible fijo. Split horizontal clásico (65% catálogo / 35% comanda fija). Tablas densas completas en inventario y finanzas. |
| **`xl` / `2xl`** | $\ge 1280\text{px}$ | Pantallas de Escritorio / Monitores All-in-One | Grid de 4 columnas en catálogo. Comanda expandida con vista de desglose de combos y modificadores. |

---

### 2. Especificación de Experiencia Táctil en Punto de Venta (POS Móvil)

```text
+-----------------------------------------------------------+
| [=] Mi Restaurante - Sucursal Central         [🟢 Online] |
+-----------------------------------------------------------+
| [ 🔍 Buscar plato...                                ] [X] |
+-----------------------------------------------------------+
| [🍔 Hamburguesas] [🍕 Pizzas] [🥤 Bebidas] [🍰 Postres] > | (Scroll horizontal suave)
+-----------------------------------------------------------+
| Vista: [⊞ Cuadrícula] [☰ Lista Compacta]                   |
+---------------------------------------------+-------------+
| Modo Cuadrícula (2 cols):                   |             |
| +---------------------+ +-----------------+ |             |
| | 🍔 Burger Clásica   | | 🍕 Pizza Muzz   | |             |
| | Bs. 35.00     [ + ] | | Bs. 45.00 [ + ] | |             |
| +---------------------+ +-----------------+ |             |
| +---------------------+ +-----------------+ |             |
| | 🍟 Papas Fritas     | | 🥤 Coca Cola 2L | |             |
| | Bs. 15.00     [ + ] | | Bs. 18.00 [ + ] | |             |
| +---------------------+ +-----------------+ |             |
+---------------------------------------------+-------------+
| 🛒 3 ÍTEMS | BS. 95.00          [ VER COMANDA / COBRAR ⬆ ]| (Floating Bottom Bar - Z:30)
+-----------------------------------------------------------+
```

#### Al presionar o deslizar hacia arriba la Barra de Comanda:
```text
+-----------------------------------------------------------+
|                                                           |
|                  [ Fondo con Backdrop ]                   |
|                                                           |
+-----------------------------------------------------------+
|                       [ === DRAG HANDLE === ]             |
| 📋 Comanda Actual (3 ítems)                     [✕ Cerrar] |
+-----------------------------------------------------------+
| 1x Hamburguesa Clásica                     Bs. 35.00      |
|    - Con queso extra (+Bs. 5.00)                          |
|    [-] [ 1 ] [+]                              [🗑️ Eliminar]|
+-----------------------------------------------------------+
| 1x Pizza Muzzarella                        Bs. 45.00      |
|    [-] [ 1 ] [+]                              [🗑️ Eliminar]|
+-----------------------------------------------------------+
| 1x Papas Fritas                            Bs. 15.00      |
|    [-] [ 1 ] [+]                              [🗑️ Eliminar]|
+-----------------------------------------------------------+
| Subtotal: Bs. 95.00 | Descuento: Bs. 0.00                 |
| TOTAL A PAGAR: Bs. 95.00                                  |
+-----------------------------------------------------------+
| [ 💰 PROCEDER AL COBRO (EFECTIVO / QR / TARJETA) ]        | (Botón primario ancho 100%)
+-----------------------------------------------------------+
```

---

### 3. Especificación de Inventario en Móvil (Stock Card View con Acordeón)

En pantallas `< 768px`, la tabla tabular de existencias se sustituye dinámicamente por tarjetas táctiles independientes:

```text
+-----------------------------------------------------------+
| [🔍 Buscar insumo...                             ] [Filtro]
+-----------------------------------------------------------+
| [ ] Mostrar solo insumos con stock crítico/agotado        |
+-----------------------------------------------------------+
| [⚠️] CARNE MOLIDA DE RES                         [KG]     |  (Borde izquierdo rojo/amarillo)
| Stock Actual: 2.4500 KG (Mínimo: 5.0000)      [ ▼ Detalle] |
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +  (Zona expandible al tocar)
| Costo Promedio: Bs. 32.50 | Valorización: Bs. 79.63       |
| Último Movimiento: 28/10/2026 18:30 (Venta POS #1024)     |
+-----------------------------------------------------------+
| [ 🌱 Alta Inicial ]               [ ⚙️ Ajustar Existencias ] | (Botones táctiles grandes)
+-----------------------------------------------------------+
| [✓] PAN PARA HAMBURGUESA                       [UNID]     |  (Borde izquierdo verde)
| Stock Actual: 85.0000 UNID (Mínimo: 20.0000)  [ ▶ Detalle] |
+-----------------------------------------------------------+
```

---

## Directrices de Diseño y Ergonomía Táctil (WCAG 2.2)

1. **Tamaño de Objetivos Táctiles (Touch Targets):**
   - Todo botón, enlace o control interactivo debe tener un área de contacto mínima de $44 \times 44\text{px}$ (o `p-2.5` / `min-h-[44px]` con espaciado mínimo de `gap-2`).
2. **Ubicación en la Zona de Alcance del Pulgar (Thumb Zone):**
   - Las acciones principales de alta frecuencia (agregar al carrito, abrir comanda, confirmar pago) se ubican estrictamente en la mitad inferior de la pantalla.
   - Las acciones destructivas o de configuración secundaria (vaciar comanda, filtros avanzados) se colocan en la parte superior o protegidas tras diálogo de confirmación.
3. **Gestos Táctiles Suaves y Microinteracciones:**
   - Despliegue de Bottom Sheet mediante transiciones CSS aceleradas por GPU (`transform: translateY(...)`, `transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)`).
   - *Backdrop* oscuro semitransparente con desenfoque (`bg-black/50 backdrop-blur-xs`) que bloquea el scroll del fondo cuando la comanda o el modal están activos.
4. **Respeto de Zonas Seguras de Dispositivos (Safe Area Insets):**
   - La barra flotante inferior incluye soporte nativo para barras de navegación de teléfonos sin marcos (`pb-[env(safe-area-inset-bottom,12px)]`).

---

## Consecuencias y Efectos

### Positivas
- **Operación 100% Móvil y Ubicua:** Habilita el uso de la aplicación en smartphones comerciales de bajo costo para meseros en salón y encargados en almacén sin requerir hardware POS especializado.
- **Cero Pérdida de Contexto en Ventas:** La comanda flotante permite al operador seguir explorando y agregando productos sin perder de vista el total de la cuenta.
- **Eficiencia en Conteo Físico:** El personal de inventario puede inspeccionar existencias y registrar ajustes rápidamente con una sola mano.
- **Coherencia con el Diseño TailwindAdmin:** Reutiliza los tokens de color (`primary`, `success`, `error`, `dark`) y el modo oscuro unificado.

### Negativas / Desafíos Técnicos
- Requiere mantenimiento de vistas condicionales o componentes responsivos adaptativos en Angular (`@if (isMobile())` o clases CSS `hidden md:block` / `block md:hidden`).
- Pruebas adicionales en emuladores y dispositivos reales con diversas densidades de píxeles y factores de forma.
