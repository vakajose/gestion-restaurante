import Dexie, { type Table, type DexieOptions } from 'dexie';
import {
  ActiveShiftEntity,
  CachedCategory,
  CachedProduct,
  PendingOrderEntity,
} from '../models/pos.models';

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
    this.version(2).stores({
      pendingOrders: '++localId, clientTransactionId, tenantId, branchId, syncStatus, createdAt, orderData',
    }).upgrade(async (tx) => {
      // Purgar de forma autónoma órdenes locales huérfanas sin tenantId de sesiones previas
      const table = tx.table('pendingOrders');
      const keysToDelete = await table.filter((o: PendingOrderEntity) => !o.tenantId).primaryKeys();
      if (keysToDelete.length > 0) {
        await table.bulkDelete(keysToDelete);
      }
    });
  }
}

export const appDb = new RestaurantPosDb();
