import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  KardexMovementDto,
  StockAdjustmentRequest,
  StockBalanceDto,
} from '../models/inventory.models';

@Injectable({
  providedIn: 'root',
})
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/inventory';

  // Signals de estado
  readonly stockList = signal<StockBalanceDto[]>([]);
  readonly kardexMovements = signal<KardexMovementDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Computed signals para métricas y KPIs
  readonly totalIngredientsCount = computed(() => this.stockList().length);
  readonly lowStockCount = computed(
    () => this.stockList().filter((item) => item.isLowStock).length
  );
  readonly totalInventoryValue = computed(() =>
    this.stockList().reduce((acc, item) => acc + (item.totalValue || 0), 0)
  );

  loadStock(branchId?: string | null): Observable<StockBalanceDto[]> {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }

    return this.http.get<StockBalanceDto[]>(`${this.baseUrl}/stock`, { params }).pipe(
      tap({
        next: (items) => {
          this.stockList.set(items);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.error?.detail || 'Error al cargar las existencias de inventario'
          );
        },
      })
    );
  }

  loadKardex(branchId?: string | null, ingredientId?: string | null): Observable<KardexMovementDto[]> {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    if (ingredientId) {
      params = params.set('ingredientId', ingredientId);
    }

    return this.http.get<KardexMovementDto[]>(`${this.baseUrl}/kardex`, { params }).pipe(
      tap({
        next: (movements) => {
          this.kardexMovements.set(movements);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.error?.detail || 'Error al cargar los movimientos del Kardex'
          );
        },
      })
    );
  }

  adjustStock(
    request: StockAdjustmentRequest,
    branchId?: string | null
  ): Observable<StockBalanceDto> {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }

    return this.http
      .post<StockBalanceDto>(`${this.baseUrl}/stock/adjust`, request, { params })
      .pipe(
        tap({
          next: (updated) => {
            this.stockList.update((list) =>
              list.map((item) =>
                item.ingredientId === updated.ingredientId ? updated : item
              )
            );
            this.successMessage.set(
              `Stock de ${updated.ingredientName} actualizado a ${updated.currentQuantity} ${updated.unitOfMeasure}`
            );
            this.isLoading.set(false);
          },
          error: (err) => {
            this.isLoading.set(false);
            this.errorMessage.set(
              err.error?.detail || 'Error al procesar el ajuste de inventario'
            );
          },
        })
      );
  }

  clearAlerts(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }
}
