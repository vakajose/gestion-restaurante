# 03. Especificación Técnica de Frontend y Modo Offline

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

## 4. Sistema de Diseño e Identidad Visual (TailwindAdmin + Tailwind CSS)
- La interfaz visual adopta la estética ejecutiva y moderna de **TailwindAdmin v2.0**:
  - Tipografía base: `DM Sans, sans-serif` con escala tipográfica estándar en unidades relativas `rem`.
  - Paleta base: Primario Azul Moderno (`#5d87ff`), Hover (`#4570ea`), Fondo suave (`#ecf2ff`), Secundario (`#49beff`), Éxito (`#13deb9`), Advertencia (`#ffae1f`), Error (`#fa896b`).
  - Superficies y Modo Oscuro: Canvas claro `#f6f9fc` y oscuro `#202938`; tarjetas en blanco `#ffffff` y oscuro `#2a3547` con bordes sutiles `#eaeff4` (dark `#333f55`).
  - Sombras multicapa difuminadas `--theme-shadow-md` para separación limpia de tarjetas.
  - Componentes estandarizados: `.card`, `.card-body`, `.btn`, `.btn-primary`, `.form-control`, `.sidebar-link` y `.activemenu`.
  - Soporte de temas Dark y Light mediante la clase `.dark` en la etiqueta raíz `<html>`.
  - Persistencia de tema con Signal reactivo en `ThemeService`, sincronizado con `localStorage` (`app_theme`), detección de `prefers-color-scheme` y la tabla `user_preferences`.
  - Detalle técnico completo documentado en `docs/sdd/05_design_system.md`.