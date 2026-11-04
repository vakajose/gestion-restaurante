import { Injectable, inject } from '@angular/core';
import { APP_DATABASE } from './app-database.token';
import { PendingOrderEntity } from '../models/pos.models';

/**
 * Data Access Layer (DAL) para la persistencia local de órdenes y reconciliación offline en Dexie.js.
 */
@Injectable({
  providedIn: 'root',
})
export class LocalOrderRepository {
  private readonly db = inject(APP_DATABASE);

  /**
   * Almacena una orden pendiente de sincronización en IndexedDB.
   */
  async addPendingOrder(order: Omit<PendingOrderEntity, 'localId'>): Promise<number> {
    return await this.db.pendingOrders.add(order as PendingOrderEntity);
  }

  /**
   * Actualiza el estado de sincronización de una orden local por su ID autoincremental.
   */
  async updateSyncStatus(localId: number, syncStatus: 'PENDING' | 'SYNCED' | 'FAILED'): Promise<number> {
    return await this.db.pendingOrders.update(localId, { syncStatus });
  }

  /**
   * Obtiene todas las órdenes locales con estado de sincronización PENDING para un tenant y sucursal específicos.
   */
  async getPendingOrders(tenantId?: string, branchId?: string): Promise<PendingOrderEntity[]> {
    const orders = await this.db.pendingOrders.where('syncStatus').equals('PENDING').toArray();
    if (!tenantId && !branchId) {
      return orders;
    }
    return orders.filter(
      (o) => (!o.tenantId || o.tenantId === tenantId) && (!branchId || o.branchId === branchId)
    );
  }

  /**
   * Obtiene el conteo reactivo de órdenes locales pendientes para un tenant y sucursal específicos.
   */
  async getPendingCount(tenantId?: string, branchId?: string): Promise<number> {
    if (!tenantId && !branchId) {
      return await this.db.pendingOrders.where('syncStatus').equals('PENDING').count();
    }
    const orders = await this.getPendingOrders(tenantId, branchId);
    return orders.length;
  }

  /**
   * Obtiene todas las órdenes almacenadas localmente sin filtro.
   */
  async getAll(): Promise<PendingOrderEntity[]> {
    return await this.db.pendingOrders.toArray();
  }

  /**
   * Limpia la tabla de órdenes pendientes (por tenant o total).
   */
  async clear(tenantId?: string): Promise<void> {
    if (tenantId) {
      const all = await this.db.pendingOrders.toArray();
      const keysToDelete = all
        .filter((o) => !o.tenantId || o.tenantId === tenantId)
        .map((o) => o.localId)
        .filter((id): id is number => id != null);

      if (typeof this.db.pendingOrders.bulkDelete === 'function') {
        await this.db.pendingOrders.bulkDelete(keysToDelete);
      } else {
        for (const k of keysToDelete) {
          if (typeof this.db.pendingOrders.delete === 'function') {
            await this.db.pendingOrders.delete(k);
          }
        }
      }
    } else {
      await this.db.pendingOrders.clear();
    }
  }
}
