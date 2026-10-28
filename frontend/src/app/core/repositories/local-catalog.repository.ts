import { Injectable, inject } from '@angular/core';
import { APP_DATABASE } from './app-database.token';
import { CachedCategory, CachedProduct } from '../models/pos.models';

/**
 * Data Access Layer (DAL) para la caché offline del catálogo de productos y categorías en Dexie.js.
 */
@Injectable({
  providedIn: 'root',
})
export class LocalCatalogRepository {
  private readonly db = inject(APP_DATABASE);

  /**
   * Reemplaza las categorías cacheadas localmente.
   */
  async saveCategories(categories: CachedCategory[]): Promise<void> {
    await this.db.cachedCategories.clear();
    await this.db.cachedCategories.bulkPut(categories);
  }

  /**
   * Obtiene todas las categorías cacheadas en IndexedDB.
   */
  async getCategories(): Promise<CachedCategory[]> {
    return await this.db.cachedCategories.toArray();
  }

  /**
   * Limpia la caché local de categorías.
   */
  async clearCategories(): Promise<void> {
    await this.db.cachedCategories.clear();
  }

  /**
   * Reemplaza los productos cacheados localmente.
   */
  async saveProducts(products: CachedProduct[]): Promise<void> {
    await this.db.cachedProducts.clear();
    await this.db.cachedProducts.bulkPut(products);
  }

  /**
   * Obtiene todos los productos cacheados en IndexedDB.
   */
  async getProducts(): Promise<CachedProduct[]> {
    return await this.db.cachedProducts.toArray();
  }

  /**
   * Limpia la caché local de productos.
   */
  async clearProducts(): Promise<void> {
    await this.db.cachedProducts.clear();
  }

  /**
   * Sincroniza atómicamente todo el catálogo local (categorías y productos).
   */
  async syncCatalog(categories: CachedCategory[], products: CachedProduct[]): Promise<void> {
    await this.saveCategories(categories);
    await this.saveProducts(products);
  }

  /**
   * Limpia todas las tablas de catálogo local.
   */
  async clearAll(): Promise<void> {
    await this.clearCategories();
    await this.clearProducts();
  }
}
