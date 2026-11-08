---
title: "03. Especificación Técnica de Frontend y Modo Offline"
category: "sdd"
tags:
  - sdd
  - frontend
  - angular22
  - zoneless
  - dexie
  - signals
last_updated: "2026-10-28"
status: "verified"
---

# 03. Especificación Técnica de Frontend y Modo Offline

> [!NOTE]
> Para la representación visual C4 de los componentes frontend y el esquema local IndexedDB, consulta:
> - **Diagrama C4 Nivel 3 (Frontend Angular):** [c4_level3_frontend_angular.md](../project/architecture/01-c4-system/c4_level3_frontend_angular.md)
> - **Esquema Local IndexedDB (Dexie.js):** [dexie_local_schema.md](../project/architecture/02-domain-and-data/dexie_local_schema.md)
> - **Secuencia de Sincronización Offline:** [sequence_pos_offline_sync.md](../project/architecture/04-behavior-and-flows/sequence_pos_offline_sync.md)

## 1. Arquitectura Angular

- **Versión:** Angular 22.
- **Paradigma de Componentes:** Componentes Standalone puros (sin `NgModule`s heredados).
- **Detección de Cambios Zoneless:** Configurado mediante `provideExperimentalZonelessChangeDetection()` en `app.config.ts` para máximo rendimiento y reducción del bundle sin depender de `zone.js`.
- **Gestión de Estado Reactiva:**
  - Angular Signals (`signal()`, `computed()`, `effect()`) para estado de componentes locales.
  - Servicios inyectables (`@Injectable({ providedIn: 'root' })`) basados en Signals para estado global de sesión, sucursal activa y conectividad.
  - **Signal Forms:** Validación y enlace reactivo en formularios de entrada rápida en POS y login.
- **Ruteo:** Rutas perezosas (Lazy loading) con `loadComponent` / `loadChildren` por área funcional:
  - `/auth`: Login adaptable y recuperación.
  - `/pos`: Terminal de punto de venta y toma de comandas rápida.
  - `/kds`: Pantalla de cocina (Kitchen Display System) optimizada para tablets táctiles.
  - `/catalog`: Administración de platos, recetas y sobreescrituras de sucursal.
  - `/inventory`: Existencias, compras a proveedores y Kardex.
  - `/finance`: Apertura/cierre de turnos de caja y aprobación de gastos menores.
  - `/analytics`: Dashboard de rentabilidad y reportes de rendimiento.

---

## 2. Estrategia Offline-First (Dexie.js / IndexedDB)

El módulo POS está diseñado para operar con cero latencia y tolerancia total a caídas de red local o internet.

### A. Esquema de Base de Datos Local (`Dexie.js`):

```typescript
import Dexie, { Table } from 'dexie';

export interface LocalProduct {
  id: string;
  code: string;
  name: string;
  categoryId: string;
  salePrice: number;
  isAvailable: boolean;
}

export interface LocalCategory {
  id: string;
  name: string;
  icon?: string;
  sortOrder: number;
}

export interface LocalActiveShift {
  id: string;
  branchId: string;
  openedBy: string;
  status: 'OPEN' | 'CLOSED';
  openedAt: string;
}

export interface PendingOrder {
  clientTransactionId: string;
  tenantId: string;
  branchId: string;
  cashShiftId?: string;
  ticketNumber: string;
  items: Array<{
    dishId: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
    notes?: string;
  }>;
  paymentMethod: 'CASH' | 'CARD' | 'QR' | 'TRANSFER';
  totalAmount: number;
  syncStatus: 'SYNC_PENDING' | 'SYNC_ERROR' | 'SYNCED';
  retryCount: number;
  lastAttemptAt?: string;
  errorMessage?: string;
  createdAt: string;
}

export class RestaurantLocalDb extends Dexie {
  products!: Table<LocalProduct, string>;
  categories!: Table<LocalCategory, string>;
  activeShift!: Table<LocalActiveShift, string>;
  pendingOrders!: Table<PendingOrder, string>;

  constructor() {
    super('RestaurantLocalDb');
    this.version(1).stores({
      products: 'id, code, categoryId, isAvailable',
      categories: 'id, sortOrder',
      activeShift: 'id, branchId',
      pendingOrders: 'clientTransactionId, syncStatus, createdAt'
    });
  }
}

export const db = new RestaurantLocalDb();
```

---

## 3. Protocolo de Reconciliación y Sincronización en Segundo Plano

### Flujo de Venta Offline:
1. Al cobrar un ticket sin conexión:
   - Se genera un UUID v4 local (`client_transaction_id`).
   - Se persiste inmediatamente en la tabla `pendingOrders` de IndexedDB con estado `SYNC_PENDING`.
   - Se emite el ticket impreso localmente o en pantalla con advertencia amigable ("Guardado localmente").
2. El servicio `SyncManagerService`:
   - Escucha los eventos `window.addEventListener('online')` y realiza un healthcheck periódico al backend.
   - Procesa secuencialmente las órdenes en cola enviándolas al endpoint `/api/v1/pos/orders` incluyendo la cabecera:
     ```http
     X-Client-Transaction-Id: <client_transaction_id>
     ```
   - Si el backend responde `201 Created` o `200 OK`, el estado local cambia a `SYNCED`.
   - Si el backend responde `409 Conflict` (idempotencia: la orden ya existía en el servidor debido a reintentos previos de red), se marca como `SYNCED` sin generar duplicidad.
   - Notifica a la interfaz mediante un Signal reactivo de estado de sincronización (`isSyncing = signal(false)`, `pendingCount = signal(0)`).

---

## 4. Sistema de Diseño, Motor de Temas en Caliente y Suite UI Kit (TailwindAdmin v3.0 — ADR-0014)
- La interfaz visual adopta la arquitectura **TailwindAdmin v3.0** ([`05_design_system.md`](05_design_system.md) / [ADR-0014](../adr/0014_tailwindadmin_v3_dynamic_theme_and_uikit.md)):
  - **Tipografía base:** `DM Sans, sans-serif` con escala tipográfica estándar en unidades relativas `rem`.
  - **Motor de Temas en Caliente (`CSS Custom Properties` + `ThemeService`):** Todos los colores semánticos (`bg-canvas`, `bg-surface`, `bg-sidebar`, `bg-topbar`, `border-border`, `bg-primary`) se enlazan a variables CSS nativas en `:root`. El usuario puede cambiar en vivo (`0ms` sin recargar) entre:
    - **Modo de Luminosidad (`themeMode`):** `light` | `dark`.
    - **Preset de Contraste de Superficie (`surfacePreset`):** `slate-clean` (Default: Canvas `#f1f5f9`, Cards/Sidebar `#ffffff`, Borde nítido `#dce3ec`), `warm-tinted` (Sidebar tinteado `#f8fafc`), o `dark-nav` (Sidebar azul noche `#1e293b` en modo claro).
    - **Paleta de Marca (`brandPalette`):** `blue` (`#5d87ff`), `aqua` (`#0074ba`), `purple` (`#763ebd`), `emerald` (`#0a7ea4`), `orange` (`#fa896b`).
    - **Modo de Colapso del Sidebar en Desktop (`sidebarCollapseMode`):** `mini-rail` (`270px` $\leftrightarrow$ `76px` solo íconos con tooltip) o `full-hide` (`270px` $\leftrightarrow$ `0px`).
  - **Suite de 8 Componentes Standalone (`src/app/shared/components/ui/`):**
    1. `<app-icon>`: Catálogo vectorial Offline-First con los trazos SVG oficiales de **Solar Icons** (CC BY 4.0) y **Tabler Icons** (MIT).
    2. `<app-page-wrapper>`: Contenedor canónico de vista (`max-w-[1536px] mx-auto p-4 sm:p-6` o `fluid`).
    3. `<app-button>`: Jerarquía semántica estricta (`primary` para Aceptar/Guardar, `neutral` para Cancelar, `danger` para acciones destructivas, con soporte `[loading]`).
    4. `<app-badge>`: Insignias con paleta de texto de alto contraste reforzado ($\ge 4.5:1$ WCAG AA) en modo claro y oscuro.
    5. `<app-card>`: Tarjeta elevada con borde nítido `border-border` y sombra multicapa `--theme-shadow-md`.
    6. `<app-modal>`: Diálogo modal accesible (`z-[100]`) con banner sticky de resumen de errores y pie estandarizado.
    7. `<app-form-field>`: Wrapper de inputs con borde rojo automático (`.has-error`) y mensaje de validación inline debajo del control.
    8. `<app-toast-portal>` + `NotificationService`: Portal global de notificaciones en `z-[9999]` conectado a `errorInterceptor` para proyectar errores `RFC 7807 ProblemDetail` por encima de cualquier modal abierto.

---

## 5. Arquitectura de Interceptores HTTP, Silent Refresh y Control de Inactividad (ADR-0011)

Bajo [ADR-0011](../adr/0011_session_lifecycle_and_silent_refresh.md), el frontend garantiza que los colaboradores del restaurante operen sin interrupciones con tokens de corta duración (15 minutos), mientras que las terminales desatendidas se bloquean automáticamente:

### A. Interceptor Funcional con Renovación Silenciosa (`authInterceptor`):

```typescript
// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, filter, switchMap, take, throwError, BehaviorSubject } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { BranchContextService } from '../services/branch-context.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('/api/v1/auth/login') || req.url.includes('/api/v1/auth/refresh')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const branchContext = inject(BranchContextService);

  const token = authService.getToken();
  const activeBranchId = branchContext.activeBranchId();

  const setHeaders: Record<string, string> = {};
  if (token) setHeaders['Authorization'] = `Bearer ${token}`;
  if (activeBranchId) setHeaders['X-Branch-Id'] = activeBranchId;

  const authReq = Object.keys(setHeaders).length > 0 ? req.clone({ setHeaders }) : req;

  return next(authReq).pipe(
    catchError((error) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !req.url.includes('/api/v1/auth/')) {
        return handle401Error(authReq, next, authService);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(req: any, next: any, authService: AuthService) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response) => {
        isRefreshing = false;
        refreshTokenSubject.next(response.token);
        return next(req.clone({ setHeaders: { Authorization: `Bearer ${response.token}` } }));
      }),
      catchError((err) => {
        isRefreshing = false;
        authService.logout();
        return throwError(() => err);
      })
    );
  } else {
    // Encola peticiones concurrentes hasta que el nuevo token esté listo
    return refreshTokenSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) => next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })))
    );
  }
}
```

### B. Servicio de Control de Inactividad (`IdleTimerService`):
- **Eventos Monitoreados con Debounce:** `['mousemove', 'keydown', 'click', 'scroll', 'touchstart']`.
- **Lectura Dinámica del Timeout:** Lee `SESSION_INACTIVITY_TIMEOUT_MINUTES` vía `ConfigurationPublicApi` (default 30 min en Admin, 120 min en POS).
- **Advertencia Preventiva:** A los $N - 2$ minutos abre `InactivityWarningModalComponent` con cuenta regresiva en vivo (120 segundos).
- **Cierre Determinado:** Si el contador llega a cero sin interacción humana, ejecuta `authService.logout()` y redirige al login con notificación.

---

## 6. Especificación de Adaptabilidad Móvil y Componentes Táctiles (ADR-0012)

Para garantizar una experiencia fluida en smartphones y tablets (`< 768px`) sin recurrir a aplicaciones nativas separadas, el frontend adopta una arquitectura de componentes adaptativos basados en Tailwind CSS y Angular Signals:

### A. Breakpoints Estandarizados (Tailwind CSS):
- `xs` (`< 640px`): Smartphones en orientación vertical (360px a 430px). Barra flotante inferior de comanda (`FloatingCartBar`), catálogo en 1 o 2 columnas, inventario en tarjetas apilables con acordeón.
- `sm` (`640px - 767px`): Smartphones grandes y mini-tablets. Catálogo en 2 columnas, tarjetas de stock con datos financieros desplegados.
- `md` (`768px - 1023px`): Tablets en orientación vertical. Sidebar en drawer off-canvas con botón hamburguesa en Topbar y backdrop oscuro.
- `lg` (`1024px - 1279px`): Tablets en orientación horizontal y TPV de mostrador. Sidebar fijo de 270px, split horizontal de dos paneles en POS (65% catálogo / 35% comanda fija).
- `xl` / `2xl` (`>= 1280px`): Monitores de escritorio y pantallas all-in-one. Catálogo en 4 columnas.

### B. Componente Reutilizable `BottomSheetComponent` (`app-bottom-sheet`):
Componente standalone que encapsula un panel deslizante desde el borde inferior de la pantalla:
- **Inputs:**
  - `isOpen: ModelSignal<boolean>` (enlace bidireccional reactivo).
  - `title: InputSignal<string>`.
  - `maxHeight: InputSignal<string>` (default `'85vh'`).
  - `dismissible: InputSignal<boolean>` (default `true`).
- **Comportamiento Táctil:**
  - Drag handle superior de $40 \times 4\text{px}$ para arrastre vertical.
  - Backdrop oscuro con desenfoque (`bg-black/50 backdrop-blur-xs`) y bloqueo de scroll en el `body`.
  - Animación GPU fluida: `transform: translateY(0%)` al abrir, `translateY(100%)` al cerrar (`transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)`).
  - Cierre por teclado (`Escape`), swipe-down o toque fuera del panel.

### C. Patrón `PosMobileLayout` en `PosComponent`:
- **Barra de Comanda Flotante (`FloatingCartBar`):**
  - Renderizado condicional en pantallas `< lg`: visible cuando `posService.cartItems().length > 0`.
  - Posicionamiento: `fixed bottom-0 left-0 right-0 z-30 p-3 bg-white dark:bg-dark border-t border-border shadow-lg flex items-center justify-between pb-[env(safe-area-inset-bottom,12px)]`.
  - Muestra conteo de platos y total acumulado con botón de llamada a la acción: `[Ver Comanda / Cobrar ⬆]`.
- **Selector Dual de Modo de Catálogo:**
  - `readonly catalogViewMode = signal<'grid' | 'list'>('grid')`.
  - Modo Cuadrícula (`grid`): `grid grid-cols-2 gap-2.5` con tarjetas táctiles cuadradas.
  - Modo Lista Compacta (`list`): Filas horizontales delgadas de $56\text{px}$ de altura con botón `+` de $44 \times 44\text{px}$ para máxima velocidad de pedido.
- **Áreas de Contacto Táctil (WCAG 2.2):** Todos los botones interactivos tienen un target táctil mínimo de $44 \times 44\text{px}$.

### D. Patrón `InventoryMobileCardView` en `InventoryComponent`:
- En pantallas `< md`:
  - Se oculta la tabla HTML (`hidden md:block`).
  - Se activa la vista de lista de tarjetas (`block md:hidden`).
  - Cada tarjeta implementa un acordeón con estado local en Signals: `expandedIngredientId = signal<string | null>(null)`.
  - Borde izquierdo de 4px con código semántico de stock: Verde (`border-l-success`), Amarillo (`border-l-warning`), Rojo (`border-l-error`).
  - Modales de ajuste y alta inicial se abren dentro de `app-bottom-sheet` para óptima interacción con teclados virtuales numéricos en dispositivos táctiles.

---

## 7. Integración de Turno en POS y Experiencia Móvil Silenciosa (ADR-0013)

### A. Invariante de Caja en `PosComponent`:
- Inyecta `FinanceService`.
- Al inicializar o cambiar sucursal activa: ejecuta `financeService.loadCurrentShift()`.
- **Estado de Caja Reactivo:**
  - Si `financeService.hasOpenShift() == false`:
    - Botones de adición al carrito y checkout deshabilitados (`disabled`).
    - Despliegue de banner accesible: `🔴 Caja Cerrada — Debes abrir un turno para comenzar a vender`.
    - Botón de acción rápida para abrir turno (solo si el colaborador tiene permiso según `finance.shift_opening_policy`).
- **Checkout Seguro:**
  - Al ejecutar `checkout()`, extrae el `id` del turno activo de `FinanceService` y lo transmite mandatoriamente en `CreateOrderRequest.cashShiftId`.

### B. Barra Móvil Silenciosa y Alerta Offline:
- **Estado Normal (Online & Sincronizado):** Cero ruido visual. No se renderizan badges estáticos que consuman espacio vertical en smartphones.
- **Estado Offline o Cola Pendiente:** Si `posService.isOnline() == false` o `posService.pendingCount() > 0`:
  - Se activa una barra de alerta superior no invasiva en color ámbar:  
    `🟠 Modo Offline (Sin conexión) · {pendingCount} órdenes locales guardadas en terminal`.
  - Mantiene habilitado el registro en Dexie.js para sincronización atómica posterior mediante `clientTransactionId` idempotente.

---

## 8. Gestión de Menús Diarios, Selección en Turno y Cupos Reactivos en POS (ADR-0015 & PB-034)

### A. Vista Administrativa de Menús Diarios (`/catalog/menus`):
- **Componente:** `CatalogMenusComponent` (Standalone).
- **Funcionalidades:**
  - Tablero de plantillas con filtro por día de la semana (`Lunes`, `Martes`, ... `Todos`).
  - Modal reactivo con Signal Forms para armar plantillas:
    - Selector de modalidad: Precio por Plato vs. Precio Cerrado de Paquete (`package_price`).
    - Clasificador por tiempos de comida (`STARTER`, `MAIN`, `DESSERT`, `BEVERAGE`, `GENERAL`).
    - Definición de cupos sugeridos de producción (`suggested_quota`).
  - Botón de clonación rápida para replicar un menú a otros días de la semana.

### B. Integración en Modal de Apertura de Turno (`CashShiftOpenModal`):
- Al abrir caja, el modal consulta `dailyMenuService.getSuggestedTemplatesForToday()`.
- Selector desplegable: `[Menú Ejecutivo de Hoy (3 tiempos) ▼]`.
- Al seleccionar una plantilla:
  - Se despliega la lista de platos con inputs numéricos para ajustar los cupos iniciales reales según lo producido en cocina (`customQuotas`).
  - Permite marcar platos no elaborados en la jornada con cupo 0 (`AGOTADO`).
- El payload enriquecido `menuTemplateId` y `customQuotas` se envía junto con `initialCash` en `POST /api/v1/finance/shifts/open`.

### C. Experiencia Híbrida en POS y Cupos Reactivos:
- **Signals de Estado en `PosStore` / `PosService`:**
  ```typescript
  activeShiftMenu = signal<ShiftMenuDto | null>(null);
  shiftMenuItems = signal<ShiftMenuItemDto[]>([]);
  displayMode = signal<'DAILY_MENU' | 'FULL_CATALOG'>('DAILY_MENU');
  selectedCourse = signal<CourseType | 'ALL'>('ALL');
  ```
- **Modo Híbrido:**
  - Por defecto se presentan las pestañas del menú diario (`Entradas`, `Segundos`, `Postres`, `Bebidas`).
  - Cada tarjeta de plato renderiza un badge semántico:
    - `Cupo: > 5`: Badge neutro/azul (`bg-info/10 text-info`).
    - `Cupo: 1 - 5`: Badge ámbar pulsante (`bg-warning/10 text-warning`).
    - `Cupo: 0`: Badge rojo `AGOTADO` (`bg-error/15 text-error`), botón disabled para prevenir selecciones erróneas.
  - Conmutador en cabecera: *"Ver Carta Completa"*, que permite alternar a todo el catálogo maestro para pedidos especiales.
- **Ampliación de Cupo en Vivo (Supervisor):**
  - Si cocina entrega una nueva tanda, un supervisor puede tocar el plato agotado y pulsar `[+ Ampliar Cupo]`.
  - Se abre `app-modal` con teclado numérico para ingresar la cantidad adicional y el motivo.
  - Invoca `POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota` y actualiza inmediatamente los signals reactivos locales y remotos.

### D. Extensión de Almacenamiento Offline en Dexie.js:
- Tabla `cachedShiftMenu`:
  ```typescript
  cachedShiftMenu: 'id, shiftId, menuId, cachedAt';
  cachedShiftMenuItems: 'id, shiftMenuAssignmentId, dishId, courseType, currentQuota, isSoldOut';
  ```
- Al sincronizar offline, el POS descuenta el cupo localmente en Dexie.js antes de emitir la orden para consistencia inmediata.

---

## 9. Personalización de Comandas, Modificadores y Notas de Cocina (ADR-0018 & PB-020)

### A. Selector Interactivo de Modificadores en POS (`PosComponent`):
- **Disparador:** Al hacer clic en un plato con `dish.modifierGroups && dish.modifierGroups.length > 0`, se abre automáticamente el modal `[data-testid="modifier-selection-modal"]`.
- **Estructura del Modal:**
  - Encabezado con nombre y precio base del plato.
  - Grupos de opciones con validación visual en tiempo real:
    - Chip/checkbox por modificador con su precio adicional (`+Bs. X.XX` o `Gratis`).
    - Validación de `minSelectable` (marca error visual si faltan opciones en grupos requeridos) y `maxSelectable` (deshabilita o desmarca si se supera el límite).
  - **Campo de Notas de Cocina:** Input/Textarea reactivo `[data-testid="item-kitchen-notes-input"]` para instrucciones de preparación ("Término bien cocido, salsa aparte").
  - **Cálculo en Caliente:** Signal computado `modifierExtraTotal = computed(...)` que suma en tiempo real los adicionales al precio base.
  - **Confirmación:** Botón `[data-testid="confirm-modifiers-btn"]` que invoca `posService.addToCart(dish, quantity, kitchenNotes, selectedModifiers)`.

### B. Carrito Reactivo y Visor de Comandas:
- **Línea de Carrito (`CartItem`):**
  - Despliega el nombre del plato en negrita.
  - Despliega la lista de modificadores con su importe individual: `+ Extra Cheddar (+Bs. 5.00)`.
  - Despliega la nota de cocina con icono culinario en estilo cursivo atenuado: `📝 Nota: Salsa aparte`.
  - Agrupación inteligente: dos platos iguales con distintos modificadores o notas se conservan como líneas separadas en el carrito.
- **Visor de Pedidos (`/cash-control` & Modal de Comanda):**
  - En la vista del turno de caja, cada orden muestra el desglose exacto de modificadores seleccionados y notas por ítem para auditoría y despacho de cocina.

### C. Almacenamiento Offline-First en Dexie.js:
- `LocalOrderItem` persiste el arreglo `selectedModifiers` y el campo `notes`.
- Al reanudar la conexión, la cola de sincronización serializa estos campos en el payload de `CreateOrderRequest` manteniendo exactitud contable e inventario.

---

## 10. Diálogo de Cancelación con PIN y Componente Reutilizable `<app-authorization-queue>` (ADR-0019 & PB-031)

### A. Diálogo de Cancelación en POS / Control de Caja:
- **Disparador:** Botón `[Cancelar Pedido]` en el detalle de la comanda en `/pos` o `/cash-control`.
- **Formulario Modal:**
  - Selector de motivo tipado (`CUSTOMER_RETRACTED`, `TYPING_ERROR`, `KITCHEN_ISSUE`, `QUALITY_COMPLAINT`, `DUPLICATE_ORDER`).
  - Toggle / Selector de vía de devolución: `Efectivo (Gaveta)` vs `Digital (Transferencia/Tarjeta)`.
  - Pregunta obligatoria: `¿El plato ya fue preparado en cocina? (SÍ / NO)`.
  - Opción de Autorización:
    - Teclado numérico táctil para ingreso del PIN de 4 a 6 dígitos del supervisor.
    - O botón alternativo *"Enviar a cola de aprobación remota"*.

### B. Componente Presentacional Puro `<app-authorization-queue>`:
- **Ubicación:** `shared/components/ui/authorization-queue/` (Standalone).
- **Entradas (Signals):**
  - `authorizations = input.required<AuthorizationDto[]>()`
  - `viewMode = input<'CASHIER' | 'SUPERVISOR'>('CASHIER')`
  - `filterType = model<string>('ALL')`
  - `filterStatus = model<string>('ALL')`
- **Salidas (Events):**
  - `approve = output<{ id: string; notes?: string }>()`
  - `reject = output<{ id: string; reason: string }>()`
  - `requestPin = output<string>()`
- **Gobernanza:** Libre de dependencias de servicios HTTP o Dexie; puramente presentacional.

---

## 11. Modalidades de Servicio, Selector Dual, Delivery Fee y Distintivos en Tickets (ADR-0020 & PB-032)

### A. Selector Dual de Modalidad (`PosComponent`):
1. **Preselección en Barra Superior (Topbar):**
   - Grupo de botones / Segmented Control en el encabezado del terminal de venta:
     - `[🍽️ Sala]` (`DINE_IN`)
     - `[🥡 Llevar]` (`TAKEOUT`)
     - `[🛍️ Recoger]` (`SCHEDULED_PICKUP` - sujeto a parámetro `pos.allow_scheduled_pickup`)
     - `[🛵 Delivery]` (`DELIVERY`)
   - Enlace reactivo directo a `selectedOrderType = signal<OrderType>('DINE_IN')`.
2. **Confirmación y Formulario Contextual en Modal de Checkout:**
   - La pasarela de cobro hereda `selectedOrderType` y permite conmutar la modalidad antes de finalizar el pago.
   - **Campos Condicionales:**
     - Si `selectedOrderType() === 'TAKEOUT'`: Muestra input `customerName` para vocear al cliente.
     - Si `selectedOrderType() === 'SCHEDULED_PICKUP'`: Muestra `customerName`, `customerPhone` e input `deliveryNotes` (con placeholder: *"Ej. Retira a las 15:30 / Empaque térmico"*).
     - Si `selectedOrderType() === 'DELIVERY'`: Muestra `customerName`, `customerPhone`, `deliveryAddress` (obligatorio), input `deliveryFee` (editable $\ge 0$, precargado con `pos.delivery_fee_default` si `pos.delivery_fee_enabled === 'true'`) y `deliveryNotes`.
   - **Cálculo Reactivo del Total:**
     - `finalTotalAmount = computed(() => subtotalCart() + (isDelivery() ? deliveryFee() : 0))`.

### B. Distintivos Visuales en Tickets Impresos y Modal de Comanda:
- Banner superior de alto impacto según modalidad:
  - `*** PARA LLEVAR ***` (Texto enmarcado o fondo destacado).
  - `*** ENCARGO PARA RECOGER ***` (Acompañado de notas de horario pactado).
  - `*** DELIVERY / DOMICILIO ***` (Acompañado de dirección, teléfono y costo de envío).
  - `CONSUMO EN SALÓN` (Comportamiento habitual).
- Inclusión del desglose de flete en la sección de totales: `COSTO DE ENVÍO: Bs. XX.XX`.

### C. Auditoría y Filtro por Modalidad en `/cash-control`:
- El visor de turnos de caja (`CashControlComponent`) incorpora selector de filtro por modalidad (`orderTypeFilter = signal<string>('ALL')`).
- Insignia (`app-badge`) coloreada por fila de pedido en la lista de ventas del turno:
  - `DINE_IN`: Azul claro / Slate
  - `TAKEOUT`: Naranja / Warning
  - `SCHEDULED_PICKUP`: Violeta / Púrpura
  - `DELIVERY`: Esmeralda / Verde
- La acción de reimpresión de comanda/ticket reproduce fielmente la modalidad y sus datos de entrega.

### D. Almacenamiento Offline-First en Dexie.js:
- La interfaz `CreateOrderRequest` y el store `pendingOrders` persisten `orderType`, `customerName`, `customerPhone`, `deliveryAddress`, `deliveryFee` y `deliveryNotes`.
- El servicio `PosService` garantiza la conservación íntegra de estos atributos al encolar pedidos offline y al sincronizarlos con el backend.




