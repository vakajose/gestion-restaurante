import { TestBed } from '@angular/core/testing';
import { APP_DATABASE } from './app-database.token';
import { LocalOrderRepository } from './local-order.repository';
import { LocalCatalogRepository } from './local-catalog.repository';
import { LocalShiftRepository } from './local-shift.repository';
import { RestaurantPosDb } from '../db/app.database';
import {
  ActiveShiftEntity,
  CachedCategory,
  CachedProduct,
  PendingOrderEntity,
} from '../models/pos.models';

function createInMemoryMockDb(): RestaurantPosDb {
  const categories: CachedCategory[] = [];
  const products: CachedProduct[] = [];
  const orders: PendingOrderEntity[] = [];
  const shifts: ActiveShiftEntity[] = [];
  let nextOrderId = 1;

  const mockDb = {
    cachedCategories: {
      async bulkPut(items: CachedCategory[]) {
        categories.push(...items);
      },
      async toArray() {
        return [...categories];
      },
      async clear() {
        categories.length = 0;
      },
    },
    cachedProducts: {
      async bulkPut(items: CachedProduct[]) {
        products.push(...items);
      },
      async toArray() {
        return [...products];
      },
      async clear() {
        products.length = 0;
      },
    },
    pendingOrders: {
      async add(item: PendingOrderEntity) {
        const entity = { ...item, localId: nextOrderId++ };
        orders.push(entity);
        return entity.localId;
      },
      async update(id: number, changes: Partial<PendingOrderEntity>) {
        const found = orders.find((o) => o.localId === id);
        if (found) {
          Object.assign(found, changes);
          return 1;
        }
        return 0;
      },
      where(field: string) {
        return {
          equals(val: unknown) {
            const filtered = orders.filter((o) => (o as unknown as Record<string, unknown>)[field] === val);
            return {
              async toArray() {
                return [...filtered];
              },
              async count() {
                return filtered.length;
              },
            };
          },
        };
      },
      async toArray() {
        return [...orders];
      },
      async clear() {
        orders.length = 0;
      },
    },
    activeShift: {
      async put(entity: ActiveShiftEntity) {
        shifts.length = 0;
        shifts.push(entity);
      },
      async toArray() {
        return [...shifts];
      },
      async clear() {
        shifts.length = 0;
      },
    },
  };

  return mockDb as unknown as RestaurantPosDb;
}

describe('Repositories (Data Access Layer for Dexie.js)', () => {
  let mockDb: RestaurantPosDb;
  let orderRepo: LocalOrderRepository;
  let catalogRepo: LocalCatalogRepository;
  let shiftRepo: LocalShiftRepository;

  beforeEach(() => {
    mockDb = createInMemoryMockDb();

    TestBed.configureTestingModule({
      providers: [
        LocalOrderRepository,
        LocalCatalogRepository,
        LocalShiftRepository,
        { provide: APP_DATABASE, useValue: mockDb },
      ],
    });

    orderRepo = TestBed.inject(LocalOrderRepository);
    catalogRepo = TestBed.inject(LocalCatalogRepository);
    shiftRepo = TestBed.inject(LocalShiftRepository);
  });

  describe('LocalOrderRepository', () => {
    it('debe agregar una orden local y retornar su ID autoincremental', async () => {
      const orderReq = {
        clientTransactionId: 'tx-100',
        syncStatus: 'PENDING' as const,
        createdAt: '2026-10-28T12:00:00Z',
        orderData: { branchId: 'b-1', paymentMethod: 'CASH', items: [] },
        ticketNumber: 'T-001',
        totalAmount: 50.0,
      };

      const localId = await orderRepo.addPendingOrder(orderReq);
      expect(localId).toBe(1);

      const pendingOrders = await orderRepo.getPendingOrders();
      expect(pendingOrders.length).toBe(1);
      expect(pendingOrders[0].clientTransactionId).toBe('tx-100');
      expect(pendingOrders[0].localId).toBe(1);
    });

    it('debe actualizar el estado de sincronización a SYNCED', async () => {
      const localId = await orderRepo.addPendingOrder({
        clientTransactionId: 'tx-200',
        syncStatus: 'PENDING',
        createdAt: '2026-10-28T12:00:00Z',
        orderData: { branchId: 'b-1', paymentMethod: 'CASH', items: [] },
        ticketNumber: 'T-002',
        totalAmount: 120.0,
      });

      const countBefore = await orderRepo.getPendingCount();
      expect(countBefore).toBe(1);

      await orderRepo.updateSyncStatus(localId, 'SYNCED');

      const countAfter = await orderRepo.getPendingCount();
      expect(countAfter).toBe(0);

      const allOrders = await orderRepo.getAll();
      expect(allOrders[0].syncStatus).toBe('SYNCED');
    });

    it('debe limpiar todas las órdenes pendientes', async () => {
      await orderRepo.addPendingOrder({
        clientTransactionId: 'tx-300',
        syncStatus: 'PENDING',
        createdAt: '2026-10-28T12:00:00Z',
        orderData: { branchId: 'b-1', paymentMethod: 'CASH', items: [] },
        ticketNumber: 'T-003',
        totalAmount: 75.0,
      });

      await orderRepo.clear();
      const allOrders = await orderRepo.getAll();
      expect(allOrders.length).toBe(0);
    });
  });

  describe('LocalCatalogRepository', () => {
    const mockCategories: CachedCategory[] = [
      { id: 'cat-1', name: 'Hamburguesas', icon: 'burger', sortOrder: 1 },
      { id: 'cat-2', name: 'Bebidas', icon: 'cup', sortOrder: 2 },
    ];

    const mockProducts: CachedProduct[] = [
      { id: 'p-1', code: 'HAMB-1', name: 'Clásica', salePrice: 35.0, isAvailable: true, categoryId: 'cat-1' },
      { id: 'p-2', code: 'BEB-1', name: 'Gaseosa', salePrice: 10.0, isAvailable: true, categoryId: 'cat-2' },
    ];

    it('debe sincronizar y recuperar categorías y productos', async () => {
      await catalogRepo.syncCatalog(mockCategories, mockProducts);

      const savedCategories = await catalogRepo.getCategories();
      const savedProducts = await catalogRepo.getProducts();

      expect(savedCategories.length).toBe(2);
      expect(savedProducts.length).toBe(2);
      expect(savedCategories[0].name).toBe('Hamburguesas');
      expect(savedProducts[0].code).toBe('HAMB-1');
    });

    it('debe limpiar el catálogo local', async () => {
      await catalogRepo.syncCatalog(mockCategories, mockProducts);
      await catalogRepo.clearAll();

      const savedCategories = await catalogRepo.getCategories();
      const savedProducts = await catalogRepo.getProducts();

      expect(savedCategories.length).toBe(0);
      expect(savedProducts.length).toBe(0);
    });
  });

  describe('LocalShiftRepository', () => {
    it('debe guardar y recuperar el turno de caja activo', async () => {
      const activeShift: ActiveShiftEntity = {
        id: 'shift-100',
        branchId: 'branch-1',
        openedAt: '2026-10-28T08:00:00Z',
        status: 'OPEN',
        openedBy: 'cajero_demo',
      };

      await shiftRepo.saveActiveShift(activeShift);
      const retrieved = await shiftRepo.getActiveShift();

      expect(retrieved).toBeDefined();
      expect(retrieved?.id).toBe('shift-100');
      expect(retrieved?.status).toBe('OPEN');
    });

    it('debe limpiar el turno de caja activo al cerrar', async () => {
      await shiftRepo.saveActiveShift({
        id: 'shift-200',
        branchId: 'branch-1',
        openedAt: '2026-10-28T08:00:00Z',
        status: 'OPEN',
      });

      await shiftRepo.clearActiveShift();
      const retrieved = await shiftRepo.getActiveShift();

      expect(retrieved).toBeUndefined();
    });
  });
});
