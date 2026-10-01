import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { PosService } from './pos.service';
import { CatalogService } from './catalog.service';
import { AuthService } from './auth.service';
import {
  CachedCategory,
  CachedProduct,
  OrderDto,
  PendingOrderEntity,
  SyncOrdersResponse,
} from '../models/pos.models';
import { DishPriceDto } from '../models/catalog.models';
import { RestaurantPosDb } from '../db/app.database';

function createMockPosDb(): RestaurantPosDb {
  const mock = {
    cachedCategories: {
      data: [] as CachedCategory[],
      async bulkPut(items: CachedCategory[]) {
        this.data = [...items];
      },
      async toArray() {
        return [...this.data];
      },
      async clear() {
        this.data = [];
      },
    },
    cachedProducts: {
      data: [] as CachedProduct[],
      async bulkPut(items: CachedProduct[]) {
        this.data = [...items];
      },
      async toArray() {
        return [...this.data];
      },
      async clear() {
        this.data = [];
      },
    },
    pendingOrders: {
      data: [] as PendingOrderEntity[],
      nextId: 1,
      async add(item: PendingOrderEntity) {
        const entity = { ...item, localId: this.nextId++ };
        this.data.push(entity);
        return entity.localId;
      },
      async update(id: number, changes: Partial<PendingOrderEntity>) {
        const found = this.data.find((d: PendingOrderEntity) => d.localId === id);
        if (found) {
          Object.assign(found, changes);
          return 1;
        }
        return 0;
      },
      where(field: string) {
        const data = this.data;
        return {
          equals(val: unknown) {
            const filtered = data.filter((d: PendingOrderEntity) => (d as unknown as Record<string, unknown>)[field] === val);
            return {
              async toArray() {
                return [...filtered];
              },
              async count() {
                return filtered.length;
              },
              async modify(changes: Partial<PendingOrderEntity>) {
                for (const item of filtered) {
                  Object.assign(item, changes);
                }
                return filtered.length;
              },
            };
          },
        };
      },
      async count() {
        return this.data.length;
      },
    },
  };

  return mock as unknown as RestaurantPosDb;
}

describe('PosService', () => {
  let service: PosService;
  let httpTesting: HttpTestingController;
  let catalogServiceSpy: { getCategories: ReturnType<typeof vi.fn>; getDishes: ReturnType<typeof vi.fn> };
  let authServiceSpy: { currentUser: ReturnType<typeof vi.fn> };


  const mockDishes: DishPriceDto[] = [
    {
      dishId: 'dish-1',
      code: 'HAMB-01',
      name: 'Hamburguesa Doble',
      categoryId: 'cat-1',
      categoryName: 'Hamburguesas',
      basePrice: 35.0,
      effectivePrice: 35.0,
      hasOverride: false,
      isAvailable: true,
    },
    {
      dishId: 'dish-2',
      code: 'BEB-01',
      name: 'Gaseosa 500ml',
      categoryId: 'cat-2',
      categoryName: 'Bebidas',
      basePrice: 10.0,
      effectivePrice: 12.0,
      hasOverride: true,
      isAvailable: true,
    },
  ];

  beforeEach(() => {
    catalogServiceSpy = {
      getCategories: vi.fn().mockReturnValue(
        of([
          { id: 'cat-1', name: 'Hamburguesas', icon: '🍔', sortOrder: 1 },
          { id: 'cat-2', name: 'Bebidas', icon: '🥤', sortOrder: 2 },
        ])
      ),
      getDishes: vi.fn().mockReturnValue(of(mockDishes)),
    };

    authServiceSpy = {
      currentUser: vi.fn().mockReturnValue({
        id: 'user-1',
        username: 'cajero',
        branchId: 'branch-uuid-100',
        tenantId: 'tenant-uuid-1',
        role: 'CASHIER',
      }),
    };

    TestBed.configureTestingModule({
      providers: [
        PosService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CatalogService, useValue: catalogServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    service = TestBed.inject(PosService);
    httpTesting = TestBed.inject(HttpTestingController);
    service.db = createMockPosDb();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe inicializarse correctamente con estados por defecto', () => {
    expect(service).toBeTruthy();
    expect(service.cartItems()).toEqual([]);
    expect(service.totalAmount()).toBe(0);
    expect(service.totalItemsCount()).toBe(0);
    expect(service.pendingCount()).toBe(0);
    expect(service.isSyncing()).toBe(false);
  });

  describe('Gestión del Carrito de Venta', () => {
    it('debe agregar un plato nuevo al carrito y calcular subtotales y totales reactivos', () => {
      service.addToCart(mockDishes[0], 2);

      const items = service.cartItems();
      expect(items.length).toBe(1);
      expect(items[0].dishId).toBe('dish-1');
      expect(items[0].name).toBe('Hamburguesa Doble');
      expect(items[0].quantity).toBe(2);
      expect(items[0].unitPrice).toBe(35.0);
      expect(items[0].subtotal).toBe(70.0);

      expect(service.totalAmount()).toBe(70.0);
      expect(service.totalItemsCount()).toBe(2);
    });

    it('debe incrementar la cantidad si se vuelve a agregar el mismo plato', () => {
      service.addToCart(mockDishes[0], 1);
      service.addToCart(mockDishes[0], 2);

      const items = service.cartItems();
      expect(items.length).toBe(1);
      expect(items[0].quantity).toBe(3);
      expect(items[0].subtotal).toBe(105.0);
      expect(service.totalAmount()).toBe(105.0);
      expect(service.totalItemsCount()).toBe(3);
    });

    it('debe permitir agregar múltiples ítems distintos', () => {
      service.addToCart(mockDishes[0], 1);
      service.addToCart(mockDishes[1], 3);

      expect(service.cartItems().length).toBe(2);
      expect(service.totalItemsCount()).toBe(4);
      expect(service.totalAmount()).toBe(71.0); // 35 + (3 * 12) = 71
    });

    it('debe modificar la cantidad de un ítem con updateQuantity(+1 / -1)', () => {
      service.addToCart(mockDishes[0], 2);
      service.updateQuantity('dish-1', 1);

      expect(service.cartItems()[0].quantity).toBe(3);
      expect(service.totalAmount()).toBe(105.0);

      service.updateQuantity('dish-1', -1);
      expect(service.cartItems()[0].quantity).toBe(2);
      expect(service.totalAmount()).toBe(70.0);
    });

    it('debe remover el ítem si la cantidad se reduce a 0 o menor', () => {
      service.addToCart(mockDishes[0], 1);
      service.updateQuantity('dish-1', -1);

      expect(service.cartItems().length).toBe(0);
      expect(service.totalAmount()).toBe(0);
      expect(service.totalItemsCount()).toBe(0);
    });

    it('debe remover un ítem directamente con removeFromCart', () => {
      service.addToCart(mockDishes[0], 1);
      service.addToCart(mockDishes[1], 1);

      service.removeFromCart('dish-1');

      expect(service.cartItems().length).toBe(1);
      expect(service.cartItems()[0].dishId).toBe('dish-2');
      expect(service.totalAmount()).toBe(12.0);
    });

    it('debe vaciar todos los ítems con clearCart', () => {
      service.addToCart(mockDishes[0], 2);
      service.addToCart(mockDishes[1], 2);
      service.clearCart();

      expect(service.cartItems()).toEqual([]);
      expect(service.totalAmount()).toBe(0);
      expect(service.totalItemsCount()).toBe(0);
    });
  });

  describe('Carga de Catálogo y Persistencia Local Dexie.js', () => {
    it('debe cargar catálogo desde CatalogService y almacenarlo en Dexie cuando está online', async () => {
      service.isOnline.set(true);

      const result = await service.loadCatalog('branch-uuid-100');

      expect(result.categories.length).toBe(2);
      expect(result.products.length).toBe(2);
      expect(service.categories().length).toBe(2);
      expect(service.products().length).toBe(2);

      const inDbCats = await service.db.cachedCategories.toArray();
      const inDbProds = await service.db.cachedProducts.toArray();
      expect(inDbCats.length).toBe(2);
      expect(inDbProds.length).toBe(2);
    });

    it('debe recurrir a Dexie local si la petición HTTP falla', async () => {
      await service.db.cachedCategories.bulkPut([
        { id: 'cat-offline', name: 'Comida Rápida', sortOrder: 1 },
      ]);
      await service.db.cachedProducts.bulkPut([
        {
          id: 'dish-offline',
          categoryId: 'cat-offline',
          code: 'OFF-01',
          name: 'Plato Offline',
          salePrice: 25.0,
          isAvailable: true,
        },
      ]);

      (catalogServiceSpy.getCategories as ReturnType<typeof vi.fn>).mockReturnValue(
        throwError(() => new Error('Error de red'))
      );

      service.isOnline.set(true);
      const result = await service.loadCatalog('branch-uuid-100');

      expect(result.categories.length).toBe(1);
      expect(result.categories[0].name).toBe('Comida Rápida');
      expect(result.products.length).toBe(1);
      expect(result.products[0].name).toBe('Plato Offline');
    });
  });

  describe('Flujo de Cobro (Checkout) y Sincronización Idempotente', () => {
    it('debe lanzar error al hacer checkout con carrito vacío', async () => {
      await expect(service.checkout('CASH')).rejects.toThrow('El carrito de venta está vacío');
    });

    it('debe cobrar orden online exitosamente, enviando X-Client-Transaction-Id y marcando como SYNCED en Dexie', async () => {
      service.isOnline.set(true);
      service.addToCart(mockDishes[0], 2);

      const checkoutPromise = service.checkout('CASH', 'Para mesa 4');
      await new Promise((r) => setTimeout(r, 0));

      const req = httpTesting.expectOne('/api/v1/pos/orders');

      expect(req.request.method).toBe('POST');
      expect(req.request.headers.has('X-Client-Transaction-Id')).toBe(true);
      expect(req.request.body.paymentMethod).toBe('CASH');
      expect(req.request.body.items.length).toBe(1);

      const mockResponse: OrderDto = {
        id: 'order-uuid-999',
        tenantId: 'tenant-uuid-1',
        branchId: 'branch-uuid-100',
        cashShiftId: null,
        ticketNumber: 'TKT-20261001-0001',
        orderStatus: 'PAID',
        paymentMethod: 'CASH',
        totalAmount: 70.0,
        clientTransactionId: req.request.body.clientTransactionId,
        notes: 'Para mesa 4',
        createdAt: '2026-10-01T00:00:00Z',
        items: [
          {
            dishId: 'dish-1',
            dishName: 'Hamburguesa Doble',
            quantity: 2,
            unitPrice: 35.0,
            subtotal: 70.0,
          },
        ],
      };

      req.flush(mockResponse, { status: 201, statusText: 'Created' });

      const finalOrder = await checkoutPromise;
      expect(finalOrder.ticketNumber).toBe('TKT-20261001-0001');
      expect(service.cartItems()).toEqual([]);

      const pendingInDb = await service.db.pendingOrders.where('syncStatus').equals('PENDING').count();
      expect(pendingInDb).toBe(0);
      expect(service.pendingCount()).toBe(0);
    });

    it('debe guardar orden en Dexie como PENDING y emitir ticket local si está en modo offline', async () => {
      service.isOnline.set(false);
      service.addToCart(mockDishes[0], 1);

      const localOrder = await service.checkout('CARD', 'Cobro offline');

      expect(localOrder.ticketNumber).toMatch(/^LOCAL-/);
      expect(localOrder.paymentMethod).toBe('CARD');
      expect(localOrder.totalAmount).toBe(35.0);
      expect(service.cartItems()).toEqual([]);

      const pendingInDb = await service.db.pendingOrders.where('syncStatus').equals('PENDING').toArray();
      expect(pendingInDb.length).toBe(1);
      expect(pendingInDb[0].syncStatus).toBe('PENDING');
      expect(service.pendingCount()).toBe(1);
    });

    it('debe reconciliar órdenes pendientes con syncPendingOrders()', async () => {
      service.isOnline.set(true);

      await service.db.pendingOrders.add({
        clientTransactionId: 'tx-offline-1',
        syncStatus: 'PENDING',
        createdAt: '2026-10-01T00:00:00Z',
        orderData: {
          clientTransactionId: 'tx-offline-1',
          branchId: 'branch-uuid-100',
          cashShiftId: null,
          paymentMethod: 'CASH',
          items: [{ dishId: 'dish-1', quantity: 1, unitPrice: 35.0 }],
        },
      });

      await service.refreshPendingCount();
      expect(service.pendingCount()).toBe(1);

      const syncPromise = service.syncPendingOrders();
      await new Promise((r) => setTimeout(r, 0));

      const req = httpTesting.expectOne('/api/v1/pos/orders/sync');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.orders.length).toBe(1);

      const syncResponse: SyncOrdersResponse = {
        totalReceived: 1,
        totalProcessed: 1,
        syncedOrders: [
          {
            id: 'synced-order-1',
            branchId: 'branch-uuid-100',
            ticketNumber: 'TKT-SYNCED-01',
            orderStatus: 'PAID',
            paymentMethod: 'CASH',
            totalAmount: 35.0,
            clientTransactionId: 'tx-offline-1',
            createdAt: '2026-10-01T00:00:00Z',
            items: [],
          },
        ],
      };

      req.flush(syncResponse);

      const result = await syncPromise;
      expect(result?.totalProcessed).toBe(1);

      const remainingPending = await service.db.pendingOrders.where('syncStatus').equals('PENDING').count();
      expect(remainingPending).toBe(0);
      expect(service.pendingCount()).toBe(0);
    });
  });
});
