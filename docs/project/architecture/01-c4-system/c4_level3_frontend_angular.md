---
title: "Diagrama C4 Nivel 3: Componentes de Frontend (Angular 22 Zoneless)"
category: "architecture"
level: "C4-Component-Frontend"
module: "frontend"
tags:
  - architecture
  - c4-model
  - frontend
  - angular-22
  - zoneless
  - signals
  - dexie
  - tailwindadmin
last_updated: "2026-10-25"
status: "verified"
---

# 🖥️ Diagrama C4 Nivel 3: Componentes de Frontend (Angular 22 Zoneless)

Este documento especifica la **Arquitectura C4 Nivel 3 (Componentes de Frontend)** del Sistema Integral de Gestión para Restaurante (POS & ERP). Modela el cliente web construido sobre **Angular 22**, con detección de cambios sin `Zone.js` (**Zoneless Change Detection**), gestión de estado puramente reactiva mediante **Angular Signals**, almacenamiento local persistente con **Dexie.js** para capacidades **Offline-First**, e interfaz basada en el sistema de diseño **TailwindAdmin v2.0**.

---

## 1. Diagrama C4 de Componentes de Frontend

```mermaid
graph TD
    subgraph Browser_Client["Navegador Web del Cliente (Angular 22 Zoneless SPA)"]
        
        %% CAPA DE VISTAS Y LAYOUT STANDALONE
        subgraph Layer_Views["Capa de Vistas & Componentes Standalone (TailwindAdmin v2.0)"]
            AppRoot["AppComponent (app-root)<br/>- Layout Responsive con Sidebar Fijo (270px)<br/>- Top Navbar con Selector Contextual de Sucursal & Theme Toggle<br/>- Contenedor Principal (router-outlet)"]
            LoginComp["LoginComponent (app-login)<br/>Ruta: /auth/login<br/>- Autenticación Adaptativa (Username/Email & Tenant Slug/NIT)<br/>- Formularios Reactivos & ProblemDetail"]
            PosComp["PosComponent (app-pos)<br/>Ruta: /pos<br/>- Terminal Táctil de Venta y Búsqueda<br/>- Carrito de Venta Reactivo<br/>- Modales de Checkout y Ticket Térmico"]
            CatComp["CatalogComponent (app-catalog)<br/>Ruta: /catalog<br/>- Pestañas: Platos, Insumos, Categorías<br/>- Precios por Sucursal y Recetas (BOM)"]
            InvComp["InventoryComponent (app-inventory)<br/>Ruta: /inventory<br/>- Monitor de Existencias Físicas y Stock Mínimo<br/>- Modal de Ajuste y Libro Mayor Kardex"]
            FinComp["FinanceComponent (app-finance)<br/>Ruta: /finance<br/>- Apertura/Cierre de Turno y Arqueo en Vivo<br/>- Solicitud y Aprobación de Gastos Menores"]
            CompanyComp["CompanyComponent (app-company)<br/>Ruta: /company<br/>- Pestañas: Datos de Empresa y Red de Sucursales<br/>- Configuración de huso horario (ADR-0006) y modales"]
            UsersComp["UsersComponent (app-users)<br/>Ruta: /users<br/>- Gestión de Personal y Roles<br/>- Asignación de sucursales y modales CRUD"]
            ProfileComp["ProfileComponent (app-profile)<br/>Ruta: /profile<br/>- Vista 'Mi Perfil' con badges humanizados<br/>- Edición de email y cambio de contraseña con Argon2id"]
        end

        %% CAPA DE ENRUTAMIENTO Y GUARDIAS
        subgraph Layer_Routing["Capa de Enrutamiento & Control de Acceso"]
            AppRoutes["app.routes.ts<br/>(Lazy Loading Standalone con loadComponent)"]
            Guards["Guardias Funcionales:<br/>- authGuard (Verifica JWT activo)<br/>- guestGuard (Redirige usuarios autenticados)<br/>- roleGuard (Verifica roles esperados)<br/>- permissionGuard (Verifica permisos granulares recurso:acción)"]
        end

        %% CAPA DE ESTADO REACTIVO (ANGULAR SIGNALS)
        subgraph Layer_Signals["Capa de Estado Reactivo (Angular Signals & Computed)"]
            SigPos["Estado de POS & Venta:<br/>- cartItems: signal&lt;CartItem[]&gt;<br/>- totalAmount: computed(() => sum(subtotales))<br/>- totalItemsCount: computed(...)<br/>- isOnline: signal&lt;boolean&gt;<br/>- pendingCount: signal&lt;number&gt;"]
            SigFin["Estado Financiero & Turno:<br/>- currentShift: signal&lt;CashShiftDto | null&gt;<br/>- hasOpenShift: computed(...)<br/>- expectedDrawerCash: computed(...)<br/>- liveDifference: computed(...)"]
            SigAuth["Estado de Autenticación:<br/>- currentUser: signal&lt;UserDto | null&gt;<br/>- isAuthenticated: computed(...)<br/>- currentRole / currentBranchId"]
            SigBranch["Estado Contextual de Sucursal:<br/>- branches: signal&lt;BranchDto[]&gt;<br/>- activeBranch: signal&lt;BranchDto | null&gt;<br/>- activeBranchId: computed(...)"]
            SigTheme["Estado Visual:<br/>- isDarkMode: signal&lt;boolean&gt;"]
        end

        %% CAPA DE SERVICIOS CORE
        subgraph Layer_Services["Capa de Servicios de Dominio (@Injectable providedIn: root)"]
            AuthSvc["AuthService<br/>- Gestión de credenciales, login/logout<br/>- Almacenamiento seguro del JWT"]
            BranchContextSvc["BranchContextService<br/>- Resolución y filtrado de sucursales autorizadas<br/>- Conmutación reactiva y persistencia en localStorage"]
            PosSvc["PosService<br/>- Lógica del carrito y cálculos<br/>- Checkout offline-first con UUID<br/>- Reconciliación de cola Dexie"]
            CatSvc["CatalogService<br/>- CRUD de categorías, platos e ingredientes<br/>- Sobreescrituras de precio por sucursal"]
            InvSvc["InventoryService<br/>- Carga de stock y movimientos de Kardex<br/>- Registro de ajustes de inventario"]
            FinSvc["FinanceService<br/>- Apertura/cierre de turnos y arqueos<br/>- Flujo de gastos de caja chica"]
            CompanySvc["CompanyService<br/>- CRUD de empresa y red de sucursales"]
            UserSvc["UserService<br/>- Gestión de personal, colaboradores y perfil propio"]
            ThemeSvc["ThemeService<br/>- Persistencia de tema claro/oscuro<br/>- Manipulación de clase .dark en &lt;html&gt;"]
        end

        %% CAPA DE INTERCEPTOR Y CLIENTE HTTP
        subgraph Layer_Network["Capa de Comunicación HTTP & Red"]
            AuthInterceptor["authInterceptor (HttpInterceptorFn)<br/>- Inyecta Authorization: Bearer &lt;token&gt;<br/>- Inyecta X-Branch-Id: &lt;activeBranchId&gt;<br/>- Propaga X-Client-Transaction-Id<br/>- Mapea ProblemDetail RFC 7807"]
            HttpClientCore["provideHttpClient(withInterceptors)<br/>(Angular HTTP Engine)"]
        end

        %% CAPA DAL (PERSISTENCIA LOCAL INDEXEDDB CON DEXIE.JS)
        subgraph Layer_DAL["Capa de Persistencia Local (Data Access Layer - Dexie.js)"]
            DexieDb["RestaurantPosDb (appDb Singleton)"]
            T_Cats[("cachedCategories<br/>id, name, sortOrder")]
            T_Prods[("cachedProducts<br/>id, categoryId, code, salePrice, isAvailable")]
            T_Orders[("pendingOrders<br/>++localId, clientTransactionId, syncStatus, createdAt, orderData")]
            T_Shift[("activeShift<br/>id, branchId, openedAt, status")]
        end
    end

    %% Navegación y Enrutamiento
    AppRoot --> AppRoutes
    AppRoutes --> Guards
    Guards --> LoginComp
    Guards --> PosComp
    Guards --> CatComp
    Guards --> InvComp
    Guards --> FinComp

    %% Conexión de Componentes con Servicios
    LoginComp --> AuthSvc
    LoginComp --> ThemeSvc
    PosComp --> PosSvc
    PosComp --> AuthSvc
    CatComp --> CatSvc
    CatComp --> AuthSvc
    InvComp --> InvSvc
    InvComp --> AuthSvc
    FinComp --> FinSvc
    FinComp --> AuthSvc
    AppRoot --> ThemeSvc
    AppRoot --> AuthSvc

    %% Servicios alimentan Estado Reactivo
    AuthSvc --> SigAuth
    PosSvc --> SigPos
    FinSvc --> SigFin
    ThemeSvc --> SigTheme

    %% Persistencia Local en Dexie.js (DAL)
    PosSvc <-->|"Cachea y Recupera Catálogo Local"| T_Cats
    PosSvc <-->|"Cachea y Recupera Catálogo Local"| T_Prods
    PosSvc <-->|"Encola Venta Offline (PENDING/SYNCED)"| T_Orders
    FinSvc <-->|"Sincroniza Turno Abierto"| T_Shift
    DexieDb --- T_Cats
    DexieDb --- T_Prods
    DexieDb --- T_Orders
    DexieDb --- T_Shift

    %% Peticiones HTTP hacia Backend
    AuthSvc --> HttpClientCore
    PosSvc --> HttpClientCore
    CatSvc --> HttpClientCore
    InvSvc --> HttpClientCore
    FinSvc --> HttpClientCore

    HttpClientCore --> AuthInterceptor
    AuthInterceptor -->|"Peticiones REST con Bearer Token & Idempotencia"| BackendAPI["Servidor Backend Spring Boot (/api/v1/*)"]
```

---

## 2. Catálogo de Componentes Standalone y Vistas

| Componente | Selector | Ruta Relativa | Guardias | Responsabilidades de UI | Servicios Inyectados |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`AppComponent`** | `app-root` | `/` (Raíz) | N/A | Marco principal con layout TailwindAdmin v2.0; Sidebar fijo de 270px con enlaces de navegación y estado de Dexie/Usuario; Topbar con selector de sucursal activa, badges de estado del sistema (`Zoneless v22`, `Offline Sync`) y botón para alternar tema. | `ThemeService`, `AuthService` |
| **`LoginComponent`** | `app-login` | `/auth/login` | `guestGuard` | Formulario centrado sin orbes fluorescentes; login adaptativo por nombre de usuario o correo electrónico con validación síncrona; visualización de errores RFC 7807 (`ProblemDetail`). | `FormBuilder`, `AuthService`, `Router`, `ThemeService` |
| **`PosComponent`** | `app-pos` | `/pos` | `authGuard` | Terminal táctil de cobro rápido; buscador en vivo y filtro por categorías; panel lateral de carrito reactivo con edición de cantidades (+/-) y notas; checkout con generación de `clientTransactionId`; modal de ticket térmico. | `PosService`, `AuthService` |
| **`CatalogComponent`** | `app-catalog` | `/catalog` | `authGuard` | Navegación por pestañas (*Platos*, *Insumos*, *Categorías*); altas y edición de recetas (BOM); sobreescritura de precios y disponibilidad por sucursal (`BranchOverrideRequest`); clonación de platos. | `CatalogService`, `AuthService` |
| **`InventoryComponent`** | `app-inventory` | `/inventory` | `authGuard` | Monitor de stock físico con alertas visuales de insumos bajo stock mínimo; modal de ajuste manual por conteo físico o mermas; historial cronológico de movimientos de Kardex con tipo y costos. | `InventoryService`, `AuthService` |
| **`FinanceComponent`** | `app-finance` | `/finance` | `authGuard` | Panel de turnos de caja y control de gastos; modal de apertura con fondo inicial; modal de cierre con arqueo ciego o conciliación en vivo contra el efectivo esperado; solicitud y aprobación de gastos menores con comprobante. | `FinanceService`, `AuthService` |

---

## 3. Matriz de Servicios Core y Estado Reactivo (Signals)

| Servicio Inyectable | Señales de Estado Primarias (`signal`) | Señales Computadas (`computed`) | Responsabilidades y Flujos |
| :--- | :--- | :--- | :--- |
| **`AuthService`** | - `currentUser: signal<UserDto \| null>`<br/>- `token: signal<string \| null>` | - `isAuthenticated: computed(() => !!this.currentUser())`<br/>- `currentRole: computed(...)`<br/>- `currentTenantId: computed(...)`<br/>- `currentBranchId: computed(...)` | Orquesta el inicio de sesión contra `/api/v1/auth/login`, persiste el token en `localStorage`, inicializa la sesión al recargar la página y provee datos de identidad para el resto de la aplicación. |
| **`PosService`** | - `cartItems: signal<CartItem[]>`<br/>- `isOnline: signal<boolean>`<br/>- `pendingCount: signal<number>`<br/>- `isSyncing: signal<boolean>`<br/>- `isLoading: signal<boolean>`<br/>- `categories: signal<CachedCategory[]>`<br/>- `products: signal<CachedProduct[]>` | - `totalAmount: computed(() => sum(item.subtotal))`<br/>- `totalItemsCount: computed(() => sum(item.quantity))` | Gestiona la selección y cálculo del carrito en memoria; carga el catálogo desde la API o Dexie si está offline; ejecuta el checkout generando un UUID idempotente y encolando la orden localmente antes de enviar a la red. |
| **`FinanceService`** | - `currentShift: signal<CashShiftDto \| null>`<br/>- `shifts: signal<CashShiftDto[]>`<br/>- `expenses: signal<ExpenseDto[]>`<br/>- `isLoading: signal<boolean>`<br/>- `errorMessage: signal<string \| null>` | - `hasOpenShift: computed(() => status === 'OPEN')`<br/>- `pendingApprovalCount: computed(...)`<br/>- `totalApprovedExpenses: computed(...)`<br/>- `expectedDrawerCash: computed(() => initial + sales - expenses)` | Administra el ciclo del turno de caja; sincroniza el turno abierto con la tabla `activeShift` de Dexie; calcula reactivamente el efectivo esperado en gaveta para el arqueo; gestiona la rendición de gastos. |
| **`InventoryService`** | - `stockSummary: signal<StockBalanceDto[]>`<br/>- `kardexMovements: signal<KardexMovementDto[]>`<br/>- `isLoading: signal<boolean>`<br/>- `errorMessage: signal<string \| null>` | - `stockList: computed(() => this.stockSummary())` | Consulta existencias físicas por sucursal, gestiona inventario inicial, registra ajustes de auditoría con motivo y consulta el libro de Kardex con trazabilidad completa. |
| **`CatalogService`** | N/A (Emite observables de respuesta y alimenta componentes). | N/A | Cliente HTTP para el catálogo maestro de platos, categorías, recetas de insumos y parametrización de precios por sucursal. |
| **`ThemeService`** | - `isDarkMode: signal<boolean>` | N/A | Alterna entre tema claro y oscuro, actualiza la clase `dark` en el elemento raíz `<html>` y persiste la preferencia del usuario en `localStorage`. |

---

## 4. Especificación de la Capa de Datos Local (IndexedDB DAL con Dexie.js)

La aplicación utiliza **Dexie.js** mediante la clase `RestaurantPosDb` (instancia singleton exportada como `appDb`) para implementar la persistencia local de la arquitectura **Offline-First**.

```typescript
export class RestaurantPosDb extends Dexie {
  cachedCategories!: Table<CachedCategory, string>;
  cachedProducts!: Table<CachedProduct, string>;
  pendingOrders!: Table<PendingOrderEntity, number>;
  activeShift!: Table<ActiveShiftEntity, string>;

  constructor(options?: DexieOptions) {
    super('RestaurantPosDb', options);
    this.version(1).stores({
      cachedCategories: 'id, name, sortOrder',
      cachedProducts: 'id, categoryId, code, name, salePrice, isAvailable',
      pendingOrders: '++localId, clientTransactionId, syncStatus, createdAt, orderData',
      activeShift: 'id, branchId, openedAt, status',
    });
  }
}
```

### Detalle de Almacenamiento Local

| Tabla Local Dexie | Clave Primaria | Índices Secundarios | Propósito en Modo Offline |
| :--- | :--- | :--- | :--- |
| **`cachedCategories`** | `id` (UUID string) | `name`, `sortOrder` | Permite al cajero navegar por las categorías del menú sin requerir conexión con el backend. |
| **`cachedProducts`** | `id` (UUID string) | `categoryId`, `code`, `name`, `salePrice`, `isAvailable` | Contiene los platos con sus precios efectivos vigentes para la sucursal, permitiendo búsquedas instantáneas y selección táctil sin latencia de red. |
| **`pendingOrders`** | `localId` (autoincremental `++`) | `clientTransactionId`, `syncStatus`, `createdAt` | **Cola Transaccional de Pedidos:** Almacena pedidos cobrados en estado `'PENDING'`. Al recuperar la conexión a Internet, un proceso en segundo plano despacha las órdenes al endpoint `/api/v1/pos/sync` y las marca como `'SYNCED'`. |
| **`activeShift`** | `id` (UUID string) | `branchId`, `openedAt`, `status` | Mantiene el registro del turno de caja vigente para validar que el cajero tenga autorización de cobro incluso cuando la red no esté disponible. |

---

## 5. Especificación del Protocolo HTTP e Interceptores

El motor de peticiones HTTP se configura centralmente en `app.config.ts` mediante `provideHttpClient(withInterceptors([authInterceptor]))`.

### Funcionalidades del `authInterceptor`

1. **Inyección de Credenciales Bearer:**
   - Para toda petición dirigida al backend (excepto `/api/v1/auth/login`), el interceptor lee el token JWT desde `AuthService.getToken()` e inyecta la cabecera:
     ```http
     Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
     ```
2. **Propagación de Idempotencia (`X-Client-Transaction-Id`):**
   - En las operaciones de venta (`POST /api/v1/pos/orders` y `POST /api/v1/pos/sync`), se genera un identificador UUID único por transacción que viaja en la cabecera HTTP:
     ```http
     X-Client-Transaction-Id: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
     ```
   - Esta cabecera garantiza que, si una petición sufre un corte de red tras haber sido procesada en el servidor, el reintento del cliente no genere una venta duplicada ni deduzca ingredientes por segunda vez.
3. **Manejo Estandarizado de Errores RFC 7807 (`ProblemDetail`):**
   - Si el backend responde con un código de error HTTP (400, 404, 409, 500), el error se captura y sus campos normalizados (`title`, `detail`, `status`) se extraen para alimentar las señales de feedback visual (`errorMessage`, notificaciones tostada o alertas contextuales).

---

## 6. Principios de Zoneless Change Detection en Angular 22

La aplicación opera bajo el paradigma moderno **Zoneless** de Angular 22:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideExperimentalZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
```

### Ventajas de la Arquitectura Zoneless en este Proyecto

1. **Eliminación del Sobrecargo de Zone.js:** Al prescindir de `zone.js`, no se interceptan indiscriminadamente los temporizadores de JavaScript, llamadas a la API de IndexedDB ni eventos del DOM, reduciendo el tamaño del bundle final y mejorando los tiempos de arranque en tablets o terminales táctiles del POS.
2. **Reactividad Fina y Predecible:** Las actualizaciones de vista se activan exclusivamente cuando las señales de Angular (`signal`, `computed`) notifican un cambio de valor o cuando se dispara un evento de usuario, eliminando ciclos de detección de cambios innecesarios en componentes que no han mutado.
3. **Coexistencia con Operaciones Asíncronas en IndexedDB:** Las transacciones locales de Dexie.js se ejecutan de manera transparente con promesas nativas de JavaScript sin necesidad de utilizar wrappers especiales para reingresar a la zona de Angular.

---

## 7. Navegación y Enlaces Relacionados

- **Nivel Superior (Contenedores):** [Diagrama C4 Nivel 2: Topología de Contenedores](./c4_level2_containers.md)
- **Backend Relacionado:** [Diagrama C4 Nivel 3: Componentes de Backend Spring Modulith](./c4_level3_backend_modulith.md)
- **Esquema Dexie Detallado:** [Esquema Local IndexedDB Dexie.js](../02-domain-and-data/dexie_local_schema.md)
- **Secuencia de Venta POS:** [Secuencia POS $\rightarrow$ Event $\rightarrow$ Kardex/Finanzas](../04-behavior-and-flows/sequence_pos_order_deduction.md)
- **Secuencia de Sincronización:** [Secuencia Offline-First e Idempotencia](../04-behavior-and-flows/sequence_pos_offline_sync.md)
- **Gobernanza SDD:** [Estándar de Diagramación SDD](../../../.agents/skills/sdd-architecture-diagramming/SKILL.md)
