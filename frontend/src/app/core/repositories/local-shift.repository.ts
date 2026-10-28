import { Injectable, inject } from '@angular/core';
import { APP_DATABASE } from './app-database.token';
import { ActiveShiftEntity } from '../models/pos.models';

/**
 * Data Access Layer (DAL) para la persistencia del turno de caja activo en Dexie.js.
 */
@Injectable({
  providedIn: 'root',
})
export class LocalShiftRepository {
  private readonly db = inject(APP_DATABASE);

  /**
   * Guarda o actualiza el turno de caja activo en IndexedDB.
   */
  async saveActiveShift(entity: ActiveShiftEntity): Promise<void> {
    await this.db.activeShift.clear();
    await this.db.activeShift.put(entity);
  }

  /**
   * Obtiene el turno de caja activo almacenado localmente, si existe.
   */
  async getActiveShift(): Promise<ActiveShiftEntity | undefined> {
    const shifts = await this.db.activeShift.toArray();
    return shifts.length > 0 ? shifts[0] : undefined;
  }

  /**
   * Elimina el turno de caja activo de IndexedDB al cerrar jornada.
   */
  async clearActiveShift(): Promise<void> {
    await this.db.activeShift.clear();
  }
}
