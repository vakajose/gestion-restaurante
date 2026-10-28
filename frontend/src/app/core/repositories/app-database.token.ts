import { InjectionToken } from '@angular/core';
import { appDb, RestaurantPosDb } from '../db/app.database';

/**
 * InjectionToken para la instancia de base de datos Dexie.js (RestaurantPosDb).
 * Permite proveer implementaciones simuladas (mocks) en pruebas unitarias y de integración.
 */
export const APP_DATABASE = new InjectionToken<RestaurantPosDb>('APP_DATABASE', {
  providedIn: 'root',
  factory: () => appDb,
});
