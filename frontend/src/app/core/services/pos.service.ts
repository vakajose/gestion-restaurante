import { Injectable, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { appDb, RestaurantPosDb } from '../db/app.database';
import {
  CartItem,
  CreateOrderRequest,
  OrderDto,
  PaymentMethod,
  PendingOrderEntity,
  SyncOrdersRequest,
  SyncOrdersResponse,
  CachedCategory,
  CachedProduct,
} from '../models/pos.models';
import { CatalogService } from './catalog.service';
import { AuthService } from './auth.service';
import { DishPriceDto } from '../models/catalog.models';

@Injectable({
  providedIn: 'root',
})
export class PosService {
  private readonly http = inject(HttpClient);
  private readonly catalogService = inject(CatalogService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  // Instancia de base de datos local Dexie (sobreescribible en tests unitarios)
  db: RestaurantPosDb = appDb;

  // Estados Reactivos con Signals
  readonly isOnline = signal<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  readonly pendingCount = signal<number>(0);
  readonly cartItems = signal<CartItem[]>([]);
  readonly isSyncing = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly categories = signal<CachedCategory[]>([]);
  readonly products = signal<CachedProduct[]>([]);

  // Totales computados reactivamente
  readonly totalAmount = computed(() =>
    Number(this.cartItems().reduce((sum, item) => sum + item.subtotal, 0).toFixed(2))
  );

  readonly totalItemsCount = computed(() =>
    this.cartItems().reduce((sum, item) => sum + item.quantity, 0)
  );

  constructor() {
    this.initNetworkListeners();
  }

  private initNetworkListeners(): void {
    if (isPlatformBrowser(this.platformId) && typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.isOnline.set(true);
        this.syncPendingOrders().catch(() => {
          // Fallo silencioso en reconexión automática; el usuario puede reintentar manualmente
        });
      });

      window.addEventListener('offline', () => {
        this.isOnline.set(false);
      });
    }
  }

  /**
   * Carga el catálogo de categorías y platos desde el backend y los guarda en Dexie.
   * Si la red falla o está offline, recupera los datos locales desde Dexie.js.
   */
  async loadCatalog(branchId?: string): Promise<{ categories: CachedCategory[]; products: CachedProduct[] }> {
    this.isLoading.set(true);
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId || undefined;

    if (this.isOnline()) {
      try {
        const [cats, dishes] = await Promise.all([
          firstValueFrom(this.catalogService.getCategories()),
          firstValueFrom(this.catalogService.getDishes(effectiveBranchId)),
        ]);

        const cachedCats: CachedCategory[] = (cats || []).map((c) => ({
          id: c.id,
          name: c.name,
          icon: c.icon,
          sortOrder: c.sortOrder ?? 0,
        }));

        const cachedProds: CachedProduct[] = (dishes || []).map((d) => ({
          id: d.dishId,
          categoryId: d.categoryId,
          code: d.code,
          name: d.name,
          description: d.description,
          salePrice: d.effectivePrice ?? d.basePrice,
          isAvailable: d.isAvailable,
          categoryName: d.categoryName,
        }));

        try {
          await this.db.cachedCategories.clear();
          await this.db.cachedCategories.bulkPut(cachedCats);
          await this.db.cachedProducts.clear();
          await this.db.cachedProducts.bulkPut(cachedProds);
        } catch {
          // Si el storage no está disponible, continuar con los datos en memoria
        }

        this.categories.set(cachedCats);
        this.products.set(cachedProds);
        await this.refreshPendingCount();
        this.isLoading.set(false);
        return { categories: cachedCats, products: cachedProds };
      } catch {
        // Fallback a Dexie si la petición de red falló
      }
    }

    // Modo Offline o fallo de red: cargar desde Dexie
    const localData = await this.loadCatalogFromDexie();
    await this.refreshPendingCount();
    this.isLoading.set(false);
    return localData;
  }

  /**
   * Lee categorías y productos almacenados en Dexie.js
   */
  async loadCatalogFromDexie(): Promise<{ categories: CachedCategory[]; products: CachedProduct[] }> {
    try {
      const cats = await this.db.cachedCategories.toArray();
      const prods = await this.db.cachedProducts.toArray();
      cats.sort((a, b) => a.sortOrder - b.sortOrder);

      this.categories.set(cats);
      this.products.set(prods);
      return { categories: cats, products: prods };
    } catch {
      this.categories.set([]);
      this.products.set([]);
      return { categories: [], products: [] };
    }
  }

  /**
   * Agrega un plato o producto al carrito reactivo
   */
  addToCart(dish: DishPriceDto | CachedProduct, quantity = 1, notes?: string): void {
    const dishId = 'dishId' in dish ? dish.dishId : dish.id;
    const unitPrice = 'effectivePrice' in dish ? dish.effectivePrice : dish.salePrice;

    this.cartItems.update((items) => {
      const existingIndex = items.findIndex((i) => i.dishId === dishId);
      if (existingIndex >= 0) {
        const existing = items[existingIndex];
        const updatedQty = existing.quantity + quantity;
        const updatedSubtotal = Number((updatedQty * existing.unitPrice).toFixed(2));
        const updatedItems = [...items];
        updatedItems[existingIndex] = {
          ...existing,
          quantity: updatedQty,
          subtotal: updatedSubtotal,
          notes: notes ?? existing.notes,
        };
        return updatedItems;
      }

      const newItem: CartItem = {
        dishId,
        code: dish.code,
        name: dish.name,
        unitPrice,
        quantity,
        subtotal: Number((quantity * unitPrice).toFixed(2)),
        notes,
      };
      return [...items, newItem];
    });
  }

  /**
   * Actualiza la cantidad de un ítem en el carrito (+1 o -1).
   * Si la cantidad resulta <= 0, elimina el ítem automáticamente.
   */
  updateQuantity(dishId: string, delta: number): void {
    this.cartItems.update((items) => {
      const existing = items.find((i) => i.dishId === dishId);
      if (!existing) {
        return items;
      }

      const newQty = existing.quantity + delta;
      if (newQty <= 0) {
        return items.filter((i) => i.dishId !== dishId);
      }

      return items.map((i) =>
        i.dishId === dishId
          ? {
              ...i,
              quantity: newQty,
              subtotal: Number((newQty * i.unitPrice).toFixed(2)),
            }
          : i
      );
    });
  }

  /**
   * Elimina un ítem específico del carrito por su dishId
   */
  removeFromCart(dishId: string): void {
    this.cartItems.update((items) => items.filter((i) => i.dishId !== dishId));
  }

  /**
   * Vacía todos los ítems del carrito de venta
   */
  clearCart(): void {
    this.cartItems.set([]);
  }

  /**
   * Procesa el cobro de la orden:
   * 1. Genera clientTransactionId (UUID idempotente).
   * 2. Persiste de inmediato en Dexie.js con syncStatus = 'PENDING'.
   * 3. Si está en línea, envía a POST /api/v1/pos/orders con cabecera X-Client-Transaction-Id.
   *    Al recibir 201/200, marca el registro en Dexie como 'SYNCED'.
   * 4. Si está offline o la red falla, mantiene en 'PENDING' y emite ticket local.
   */
  async checkout(
    paymentMethod: PaymentMethod,
    notes?: string,
    branchId?: string,
    cashShiftId?: string | null
  ): Promise<OrderDto> {
    const currentCart = this.cartItems();
    if (currentCart.length === 0) {
      throw new Error('El carrito de venta está vacío');
    }

    const effectiveBranchId =
      branchId || this.authService.currentUser()?.branchId || '00000000-0000-0000-0000-000000000000';
    const clientTxId =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `tx-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const currentTotal = this.totalAmount();
    const nowIso = new Date().toISOString();
    const localTicketNumber = `LOCAL-${clientTxId.substring(0, 8).toUpperCase()}`;

    const orderRequest: CreateOrderRequest = {
      clientTransactionId: clientTxId,
      branchId: effectiveBranchId,
      cashShiftId: cashShiftId || null,
      paymentMethod,
      notes: notes?.trim() || undefined,
      items: currentCart.map((i) => ({
        dishId: i.dishId,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        notes: i.notes,
      })),
    };

    let localId: number | undefined;
    try {
      const pendingEntity: PendingOrderEntity = {
        clientTransactionId: clientTxId,
        syncStatus: 'PENDING',
        createdAt: nowIso,
        orderData: orderRequest,
        ticketNumber: localTicketNumber,
        totalAmount: currentTotal,
      };
      localId = await this.db.pendingOrders.add(pendingEntity);
    } catch {
      // Continuar si IndexedDB no responde
    }

    await this.refreshPendingCount();
    this.clearCart();

    // Intento en línea
    if (this.isOnline()) {
      try {
        const headers = new HttpHeaders({
          'X-Client-Transaction-Id': clientTxId,
        });

        const serverOrder = await firstValueFrom(
          this.http.post<OrderDto>('/api/v1/pos/orders', orderRequest, { headers })
        );

        if (localId != null) {
          try {
            await this.db.pendingOrders.update(localId, { syncStatus: 'SYNCED' });
          } catch {
            // No bloqueante
          }
        }

        await this.refreshPendingCount();
        return serverOrder;
      } catch {
        // Fallback a ticket local offline
      }
    }

    // Emisión de comprobante local offline
    const localOrder: OrderDto = {
      id: clientTxId,
      branchId: effectiveBranchId,
      cashShiftId: cashShiftId || null,
      ticketNumber: localTicketNumber,
      orderStatus: 'PAID',
      paymentMethod,
      totalAmount: currentTotal,
      clientTransactionId: clientTxId,
      notes: notes?.trim(),
      createdAt: nowIso,
      items: currentCart.map((i, idx) => ({
        id: `local-item-${idx + 1}`,
        dishId: i.dishId,
        dishName: i.name,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        subtotal: i.subtotal,
        notes: i.notes,
      })),
    };

    return localOrder;
  }

  /**
   * Lee todas las órdenes 'PENDING' de Dexie.js e invoca POST /api/v1/pos/orders/sync.
   * Marca las órdenes procesadas como 'SYNCED' y actualiza el contador.
   */
  async syncPendingOrders(): Promise<SyncOrdersResponse | null> {
    if (this.isSyncing() || !this.isOnline()) {
      return null;
    }

    this.isSyncing.set(true);
    try {
      const pending = await this.db.pendingOrders.where('syncStatus').equals('PENDING').toArray();
      if (!pending || pending.length === 0) {
        await this.refreshPendingCount();
        return null;
      }

      const syncRequest: SyncOrdersRequest = {
        orders: pending.map((p) => p.orderData),
      };

      const response = await firstValueFrom(
        this.http.post<SyncOrdersResponse>('/api/v1/pos/orders/sync', syncRequest)
      );

      for (const p of pending) {
        if (p.localId != null) {
          try {
            await this.db.pendingOrders.update(p.localId, { syncStatus: 'SYNCED' });
          } catch {
            // Ignorar fallas individuales al actualizar Dexie
          }
        }
      }

      await this.refreshPendingCount();
      return response;
    } finally {
      this.isSyncing.set(false);
    }
  }

  /**
   * Actualiza el signal pendingCount consultando IndexedDB
   */
  async refreshPendingCount(): Promise<number> {
    try {
      const count = await this.db.pendingOrders.where('syncStatus').equals('PENDING').count();
      this.pendingCount.set(count);
      return count;
    } catch {
      return 0;
    }
  }
}
