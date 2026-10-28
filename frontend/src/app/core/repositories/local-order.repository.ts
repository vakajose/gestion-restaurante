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
   * Obtiene todas las órdenes locales con estado de sincronización PENDING.
   */
  async getPendingOrders(): Promise<PendingOrderEntity[]> {
    return await this.db.pendingOrders.where('syncStatus').equals('PENDING').toArray();
  }

  /**
   * Obtiene el conteo reactivo de órdenes locales pendientes.
   */
  async getPendingCount(): Promise<number> {
    return await this.db.pendingOrders.where('syncStatus').equals('PENDING').count();
  }

  /**
   * Obtiene todas las órdenes almacenadas localmente sin filtro.
   */
  async getAll(): Promise<PendingOrderEntity[]> {
    return await this.db.pendingOrders.toArray();
  }

  /**
   * Limpia la tabla de órdenes pendientes (usado en tests y resets).
   */
  async clear(): Promise<void> {
    await this.db.pendingOrders.clear();
  }
}
