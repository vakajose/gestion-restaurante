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
  }
}

export const appDb = new RestaurantPosDb();
