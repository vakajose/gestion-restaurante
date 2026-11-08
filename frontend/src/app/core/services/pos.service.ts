import { Injectable, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { RestaurantPosDb } from '../db/app.database';
import { LocalOrderRepository } from '../repositories/local-order.repository';
import { LocalCatalogRepository } from '../repositories/local-catalog.repository';
import { LocalShiftRepository } from '../repositories/local-shift.repository';
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
  PromotionDto,
  CreatePromotionRequest,
  SelectedModifierRequest,
} from '../models/pos.models';
import { CatalogService } from './catalog.service';
import { AuthService } from './auth.service';
import { DishPriceDto, ComboDto } from '../models/catalog.models';
import { Observable } from 'rxjs';

function areModifiersIdentical(
  a?: SelectedModifierRequest[],
  b?: SelectedModifierRequest[]
): boolean {
  const listA = a || [];
  const listB = b || [];
  if (listA.length !== listB.length) return false;
  if (listA.length === 0 && listB.length === 0) return true;
  const idsA = listA.map((m) => m.modifierId).sort();
  const idsB = listB.map((m) => m.modifierId).sort();
  return idsA.every((id, idx) => id === idsB[idx]);
}

@Injectable({
  providedIn: 'root',
})
export class PosService {
  private readonly http = inject(HttpClient);
  private readonly catalogService = inject(CatalogService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly localOrderRepo = inject(LocalOrderRepository);
  readonly localCatalogRepo = inject(LocalCatalogRepository);
  readonly localShiftRepo = inject(LocalShiftRepository);

  /**
   * Getter/setter para compatibilidad con suites de pruebas unitarias existentes.
   */
  get db(): RestaurantPosDb {
    return (this.localOrderRepo as unknown as { db: RestaurantPosDb }).db;
  }
  set db(mock: RestaurantPosDb) {
    (this.localOrderRepo as unknown as { db: RestaurantPosDb }).db = mock;
    (this.localCatalogRepo as unknown as { db: RestaurantPosDb }).db = mock;
    if (this.localShiftRepo as unknown as { db: RestaurantPosDb }) {
      (this.localShiftRepo as unknown as { db: RestaurantPosDb }).db = mock;
    }
  }

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
  readonly activePromotions = signal<PromotionDto[]>([]);
  readonly selectedPromotion = signal<PromotionDto | null>(null);

  // Promoción efectiva: la seleccionada manualmente o la primera automática activa
  readonly effectivePromotion = computed<PromotionDto | null>(() => {
    const selected = this.selectedPromotion();
    if (selected) return selected;
    const autos = this.activePromotions().filter((p) => p.applicationMode === 'AUTOMATIC');
    return autos.length > 0 ? autos[0] : null;
  });

  // Subtotal bruto sin descuentos
  readonly rawSubtotal = computed(() =>
    Number(this.cartItems().reduce((sum, item) => sum + item.subtotal, 0).toFixed(2))
  );

  // Descuento calculado según la promoción vigente
  readonly discountAmount = computed(() => {
    const promo = this.effectivePromotion();
    const subtotal = this.rawSubtotal();
    const items = this.cartItems();
    if (!promo || items.length === 0 || subtotal <= 0) return 0;
    if (promo.minOrderAmount && subtotal < promo.minOrderAmount) return 0;

    if (promo.scope === 'GLOBAL') {
      if (promo.discountType === 'PERCENTAGE') {
        return Number((subtotal * (promo.discountValue / 100)).toFixed(2));
      } else if (promo.discountType === 'FIXED_AMOUNT') {
        return Math.min(promo.discountValue, subtotal);
      }
    } else if (promo.scope === 'DISH') {
      let dishDiscount = 0;
      for (const item of items) {
        if (item.dishId === promo.targetDishId) {
          if (promo.discountType === 'PERCENTAGE') {
            dishDiscount += Number((item.subtotal * (promo.discountValue / 100)).toFixed(2));
          } else if (promo.discountType === 'FIXED_AMOUNT') {
            dishDiscount += Math.min(promo.discountValue, item.subtotal);
          }
        }
      }
      return Number(Math.min(dishDiscount, subtotal).toFixed(2));
    }
    return 0;
  });

  // Total neto a cobrar descontando promociones
  readonly totalAmount = computed(() => {
    const subtotal = this.rawSubtotal();
    const discount = this.discountAmount();
    return Number(Math.max(0, subtotal - discount).toFixed(2));
  });

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
          modifierGroups: d.modifierGroups,
        }));

        try {
          await this.localCatalogRepo.syncCatalog(cachedCats, cachedProds);
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
      const cats = await this.localCatalogRepo.getCategories();
      const prods = await this.localCatalogRepo.getProducts();
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
   * Agrega un plato o producto al carrito reactivo con soporte opcional para modificadores/extras
   */
  addToCart(
    dish: DishPriceDto | CachedProduct,
    quantity = 1,
    notes?: string,
    selectedModifiers?: SelectedModifierRequest[]
  ): void {
    const dishId = 'dishId' in dish ? dish.dishId : dish.id;
    const baseUnitPrice = 'effectivePrice' in dish ? dish.effectivePrice : dish.salePrice;
    const extraPrice = (selectedModifiers || []).reduce((sum, m) => sum + (m.additionalPrice ?? m.unitPrice ?? 0), 0);
    const unitPrice = Number((baseUnitPrice + extraPrice).toFixed(2));
    const normalizedNotes = notes?.trim() || undefined;
    const cleanModifiers = selectedModifiers && selectedModifiers.length > 0 ? selectedModifiers : undefined;

    this.cartItems.update((items) => {
      // Ítems iguales con distintas notas o modificadores se agregan como líneas separadas
      const existingIndex = items.findIndex((i) => {
        if (i.dishId !== dishId || i.isCombo) return false;
        const sameNotes = (i.notes?.trim() || '') === (normalizedNotes || '');
        const sameModifiers = areModifiersIdentical(i.selectedModifiers, cleanModifiers);
        return sameNotes && sameModifiers;
      });

      if (existingIndex >= 0) {
        const existing = items[existingIndex];
        const updatedQty = existing.quantity + quantity;
        const updatedSubtotal = Number((updatedQty * existing.unitPrice).toFixed(2));
        const updatedItems = [...items];
        updatedItems[existingIndex] = {
          ...existing,
          quantity: updatedQty,
          subtotal: updatedSubtotal,
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
        notes: normalizedNotes,
        selectedModifiers: cleanModifiers,
      };
      return [...items, newItem];
    });
  }

  /**
   * Agrega un combo gastronómico al carrito reactivo
   */
  addComboToCart(
    combo: ComboDto,
    quantity = 1,
    selectedDishIds?: string[],
    notes?: string
  ): void {
    const defaultDishId =
      combo.items.find((i) => i.defaultDishId)?.defaultDishId ||
      combo.items[0]?.options[0]?.dishId ||
      combo.id;

    const components =
      selectedDishIds && selectedDishIds.length > 0
        ? selectedDishIds
        : combo.items.map((i) => i.defaultDishId).filter((id): id is string => !!id);

    const effectiveNotes = notes || `Pack: ${combo.name}`;
    const comboCartItem: CartItem = {
      dishId: defaultDishId,
      code: combo.code,
      name: combo.name,
      unitPrice: combo.basePrice,
      quantity,
      subtotal: Number((quantity * combo.basePrice).toFixed(2)),
      notes: effectiveNotes,
      comboId: combo.id,
      componentDishIds: components,
      isCombo: true,
    };

    this.cartItems.update((items) => {
      const existingIndex = items.findIndex(
        (i) => i.comboId === combo.id && i.notes === effectiveNotes
      );
      if (existingIndex >= 0) {
        const existing = items[existingIndex];
        const updatedQty = existing.quantity + quantity;
        const updatedSubtotal = Number((updatedQty * existing.unitPrice).toFixed(2));
        const updatedItems = [...items];
        updatedItems[existingIndex] = {
          ...existing,
          quantity: updatedQty,
          subtotal: updatedSubtotal,
        };
        return updatedItems;
      }
      return [...items, comboCartItem];
    });
  }

  /**
   * Actualiza la cantidad de un ítem en el carrito (+1 o -1).
   * Si la cantidad resulta <= 0, elimina el ítem automáticamente.
   */
  updateQuantity(dishId: string, delta: number, index?: number): void {
    this.cartItems.update((items) => {
      if (index !== undefined && index >= 0 && index < items.length) {
        const item = items[index];
        const newQty = item.quantity + delta;
        if (newQty <= 0) {
          return items.filter((_, idx) => idx !== index);
        }
        const updated = [...items];
        updated[index] = {
          ...item,
          quantity: newQty,
          subtotal: Number((newQty * item.unitPrice).toFixed(2)),
        };
        return updated;
      }

      const existingIndex = items.findIndex((i) => i.dishId === dishId);
      if (existingIndex < 0) {
        return items;
      }
      const existing = items[existingIndex];
      const newQty = existing.quantity + delta;
      if (newQty <= 0) {
        return items.filter((_, idx) => idx !== existingIndex);
      }
      const updated = [...items];
      updated[existingIndex] = {
        ...existing,
        quantity: newQty,
        subtotal: Number((newQty * existing.unitPrice).toFixed(2)),
      };
      return updated;
    });
  }

  /**
   * Elimina un ítem específico del carrito por su dishId o índice de línea
   */
  removeFromCart(dishId: string, index?: number): void {
    this.cartItems.update((items) => {
      if (index !== undefined && index >= 0 && index < items.length) {
        return items.filter((_, idx) => idx !== index);
      }
      return items.filter((i) => i.dishId !== dishId);
    });
  }

  /**
   * Vacía todos los ítems del carrito de venta
   */
  clearCart(): void {
    this.cartItems.set([]);
    this.selectedPromotion.set(null);
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
    cashShiftId?: string | null,
    orderType?: 'DINE_IN' | 'TAKEOUT' | 'DELIVERY',
    customerName?: string
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

    let effectiveCashShiftId = cashShiftId || null;
    if (!effectiveCashShiftId) {
      try {
        const cached = await this.localShiftRepo.getActiveShift();
        if (cached && cached.status === 'OPEN') {
          effectiveCashShiftId = cached.id;
        }
      } catch {
        // Dexie offline fallback
      }
    }

    const promo = this.effectivePromotion();
    const orderRequest: CreateOrderRequest = {
      clientTransactionId: clientTxId,
      branchId: effectiveBranchId,
      cashShiftId: effectiveCashShiftId,
      paymentMethod,
      notes: notes?.trim() || undefined,
      items: currentCart.map((i) => {
        const extraPrice = (i.selectedModifiers || []).reduce(
          (sum, m) => sum + (m.additionalPrice ?? m.unitPrice ?? 0),
          0
        );
        const basePrice = Number((i.unitPrice - extraPrice).toFixed(2));
        return {
          dishId: i.dishId,
          quantity: i.quantity,
          unitPrice: basePrice >= 0 ? basePrice : i.unitPrice,
          notes: i.notes,
          comboId: i.comboId,
          componentDishIds: i.componentDishIds,
          discountAmount: i.discountAmount,
          selectedModifiers: i.selectedModifiers?.map((m) => ({
            ...m,
            modifierId: m.modifierId,
            modifierName: m.modifierName || m.name || '',
            name: m.name || m.modifierName || '',
            additionalPrice: m.additionalPrice ?? m.unitPrice ?? 0,
            unitPrice: m.unitPrice ?? m.additionalPrice ?? 0,
            ingredientId: m.ingredientId ?? null,
            quantityConsumed: m.quantityConsumed ?? 0,
          })),
        };
      }),
      appliedPromotionId: promo?.id ?? null,
      orderType: orderType || 'DINE_IN',
      customerName: customerName?.trim() || undefined,
    };

    let localId: number | undefined;
    const currentTenantId = this.authService.currentUser()?.tenantId;
    try {
      const pendingEntity: PendingOrderEntity = {
        clientTransactionId: clientTxId,
        tenantId: currentTenantId,
        branchId: effectiveBranchId,
        syncStatus: 'PENDING',
        createdAt: nowIso,
        orderData: orderRequest,
        ticketNumber: localTicketNumber,
        totalAmount: currentTotal,
      };
      localId = await this.localOrderRepo.addPendingOrder(pendingEntity);
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
            await this.localOrderRepo.updateSyncStatus(localId, 'SYNCED');
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
      cashShiftId: effectiveCashShiftId,
      cashierId: this.authService.currentUser()?.id || null,
      ticketNumber: localTicketNumber,
      orderStatus: 'PAID',
      paymentMethod,
      totalAmount: currentTotal,
      clientTransactionId: clientTxId,
      notes: notes?.trim(),
      orderType: orderType || 'DINE_IN',
      customerName: customerName?.trim() || undefined,
      createdAt: nowIso,
      items: currentCart.map((i, idx) => ({
        id: `local-item-${idx + 1}`,
        dishId: i.dishId,
        dishName: i.name,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        subtotal: i.subtotal,
        notes: i.notes,
        modifiers: i.selectedModifiers,
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

    const currentTenantId = this.authService.currentUser()?.tenantId;
    if (!currentTenantId) {
      return null;
    }

    this.isSyncing.set(true);
    try {
      const pending = await this.localOrderRepo.getPendingOrders(currentTenantId);
      if (!pending || pending.length === 0) {
        await this.refreshPendingCount();
        return null;
      }

      const syncRequest: SyncOrdersRequest = {
        orders: pending.map((p) => ({
          ...p.orderData,
          items: p.orderData.items.map((it) => ({
            ...it,
            selectedModifiers: it.selectedModifiers?.map((m) => ({
              ...m,
              name: m.name || m.modifierName,
              unitPrice: m.unitPrice ?? m.additionalPrice ?? 0,
            })),
          })),
        })),
      };

      const response = await firstValueFrom(
        this.http.post<SyncOrdersResponse>('/api/v1/pos/orders/sync', syncRequest)
      );

      for (const p of pending) {
        if (p.localId != null) {
          try {
            await this.localOrderRepo.updateSyncStatus(p.localId, 'SYNCED');
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
   * Actualiza el signal pendingCount consultando IndexedDB para el tenant actual
   */
  async refreshPendingCount(): Promise<number> {
    try {
      const currentTenantId = this.authService.currentUser()?.tenantId;
      if (!currentTenantId) {
        this.pendingCount.set(0);
        return 0;
      }
      const count = await this.localOrderRepo.getPendingCount(currentTenantId);
      this.pendingCount.set(count);
      return count;
    } catch {
      return 0;
    }
  }

  /**
   * Limpia toda la cola de órdenes offline de IndexedDB para el tenant actual.
   */
  async clearOfflineOrdersQueue(): Promise<void> {
    try {
      const currentTenantId = this.authService.currentUser()?.tenantId;
      await this.localOrderRepo.clear(currentTenantId);
      this.pendingCount.set(0);
    } catch {
      // Ignorar fallas al limpiar Dexie
    }
  }

  /**
   * Obtiene promociones activas para la sucursal desde el servidor
   */
  getActivePromotions(branchId?: string): Observable<PromotionDto[]> {
    const params = branchId ? `?branchId=${branchId}` : '';
    return this.http.get<PromotionDto[]>(`/api/v1/pos/promotions/active${params}`);
  }

  /**
   * Carga promociones activas para la sucursal y actualiza el signal reactivo
   */
  async loadActivePromotions(branchId?: string): Promise<PromotionDto[]> {
    if (!this.isOnline()) {
      return this.activePromotions();
    }
    try {
      const promos = await firstValueFrom(this.getActivePromotions(branchId));
      this.activePromotions.set(promos || []);
      return promos || [];
    } catch {
      return this.activePromotions();
    }
  }

  /**
   * Crea una nueva promoción temporal parametrizable
   */
  createPromotion(request: CreatePromotionRequest): Observable<PromotionDto> {
    return this.http.post<PromotionDto>('/api/v1/pos/promotions', request);
  }
}
