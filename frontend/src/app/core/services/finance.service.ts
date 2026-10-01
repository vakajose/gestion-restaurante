import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import {
  CashShiftDto,
  CashShiftSummaryDto,
  CloseShiftRequest,
  CreateExpenseRequest,
  ExpenseApprovalRequest,
  ExpenseDto,
  OpenShiftRequest,
} from '../models/finance.models';
import { AuthService } from './auth.service';
import { appDb, RestaurantPosDb } from '../db/app.database';
import { ActiveShiftEntity } from '../models/pos.models';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = '/api/v1/finance';

  db: RestaurantPosDb = appDb;

  // Estados Reactivos con Signals
  readonly currentShift = signal<CashShiftDto | null>(null);
  readonly shifts = signal<CashShiftDto[]>([]);
  readonly expenses = signal<ExpenseDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Estados Computados
  readonly hasOpenShift = computed(() => this.currentShift()?.status === 'OPEN');

  readonly pendingApprovalCount = computed(
    () => this.expenses().filter((e) => e.approvalStatus === 'PENDING_APPROVAL').length
  );

  readonly totalApprovedExpenses = computed(() =>
    this.expenses()
      .filter((e) => e.approvalStatus === 'APPROVED' && e.paidFromCashDrawer)
      .reduce((sum, e) => sum + (e.amount || 0), 0)
  );

  readonly expectedDrawerCash = computed(() => {
    const shift = this.currentShift();
    if (!shift) return 0;
    if (shift.expectedCash != null) return shift.expectedCash;
    const initial = shift.initialCash || 0;
    const sales = shift.totalCashSales || 0;
    const expenses = shift.totalCashExpenses || 0;
    return Number((initial + sales - expenses).toFixed(2));
  });

  /**
   * Carga el turno de caja actualmente abierto para la sucursal activa.
   */
  loadCurrentShift(branchId?: string): Observable<CashShiftDto | null> {
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    if (!effectiveBranchId) {
      this.currentShift.set(null);
      return of(null);
    }

    this.isLoading.set(true);
    const params = new HttpParams().set('branchId', effectiveBranchId);

    return this.http.get<CashShiftDto>(`${this.baseUrl}/shifts/current`, { params }).pipe(
      tap((shift) => {
        this.currentShift.set(shift || null);
        this.isLoading.set(false);
        this.errorMessage.set(null);
        if (shift && shift.status === 'OPEN') {
          this.syncActiveShiftToDexie(shift);
        } else {
          this.clearActiveShiftFromDexie();
        }
      }),
      catchError((err) => {
        this.isLoading.set(false);
        if (err.status === 204) {
          this.currentShift.set(null);
          this.clearActiveShiftFromDexie();
          return of(null);
        }
        this.errorMessage.set('No se pudo cargar el turno actual.');
        return of(null);
      })
    );
  }

  /**
   * Abre un nuevo turno de caja.
   */
  openShift(request: OpenShiftRequest): Observable<CashShiftDto> {
    this.isLoading.set(true);
    return this.http.post<CashShiftDto>(`${this.baseUrl}/shifts/open`, request).pipe(
      tap((newShift) => {
        this.currentShift.set(newShift);
        this.shifts.update((list) => [newShift, ...list.filter((s) => s.id !== newShift.id)]);
        this.isLoading.set(false);
        this.errorMessage.set(null);
        this.syncActiveShiftToDexie(newShift);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al abrir el turno de caja';
        this.errorMessage.set(msg);
        throw err;
      })
    );
  }

  /**
   * Cierra el turno de caja actual y asienta el arqueo.
   */
  closeShift(shiftId: string, request: CloseShiftRequest): Observable<CashShiftDto> {
    this.isLoading.set(true);
    return this.http.post<CashShiftDto>(`${this.baseUrl}/shifts/${shiftId}/close`, request).pipe(
      tap((closedShift) => {
        this.currentShift.set(null);
        this.shifts.update((list) =>
          list.map((s) => (s.id === closedShift.id ? closedShift : s))
        );
        this.isLoading.set(false);
        this.errorMessage.set(null);
        this.clearActiveShiftFromDexie();
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al cerrar el turno';
        this.errorMessage.set(msg);
        throw err;
      })
    );
  }

  /**
   * Carga el historial de turnos de caja para la sucursal.
   */
  loadShifts(branchId?: string): Observable<CashShiftDto[]> {
    this.isLoading.set(true);
    let params = new HttpParams();
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    if (effectiveBranchId) {
      params = params.set('branchId', effectiveBranchId);
    }

    return this.http.get<CashShiftDto[]>(`${this.baseUrl}/shifts`, { params }).pipe(
      tap((data) => {
        this.shifts.set(data || []);
        this.isLoading.set(false);
      }),
      catchError(() => {
        this.isLoading.set(false);
        this.errorMessage.set('Error al cargar historial de turnos.');
        return of([]);
      })
    );
  }

  /**
   * Obtiene el desglose consolidado de un turno (órdenes, gastos aprobados y pendientes).
   */
  getShiftSummary(shiftId: string): Observable<CashShiftSummaryDto> {
    return this.http.get<CashShiftSummaryDto>(`${this.baseUrl}/shifts/${shiftId}/summary`);
  }

  /**
   * Carga los gastos de caja chica con filtros opcionales.
   */
  loadExpenses(branchId?: string, status?: string): Observable<ExpenseDto[]> {
    this.isLoading.set(true);
    let params = new HttpParams();
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    if (effectiveBranchId) {
      params = params.set('branchId', effectiveBranchId);
    }
    if (status && status !== 'ALL') {
      params = params.set('approvalStatus', status);
    }

    return this.http.get<ExpenseDto[]>(`${this.baseUrl}/expenses`, { params }).pipe(
      tap((data) => {
        this.expenses.set(data || []);
        this.isLoading.set(false);
      }),
      catchError(() => {
        this.isLoading.set(false);
        this.errorMessage.set('Error al cargar gastos.');
        return of([]);
      })
    );
  }

  /**
   * Registra un nuevo gasto de caja menor.
   */
  createExpense(request: CreateExpenseRequest): Observable<ExpenseDto> {
    this.isLoading.set(true);
    return this.http.post<ExpenseDto>(`${this.baseUrl}/expenses`, request).pipe(
      tap((created) => {
        this.expenses.update((list) => [created, ...list]);
        this.isLoading.set(false);
        this.errorMessage.set(null);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al registrar el gasto';
        this.errorMessage.set(msg);
        throw err;
      })
    );
  }

  /**
   * Aprueba o rechaza una solicitud de gasto menor.
   */
  approveExpense(expenseId: string, request: ExpenseApprovalRequest): Observable<ExpenseDto> {
    this.isLoading.set(true);
    return this.http.post<ExpenseDto>(`${this.baseUrl}/expenses/${expenseId}/approve`, request).pipe(
      tap((updated) => {
        this.expenses.update((list) => list.map((e) => (e.id === updated.id ? updated : e)));
        this.isLoading.set(false);
        this.errorMessage.set(null);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al procesar la aprobación del gasto';
        this.errorMessage.set(msg);
        throw err;
      })
    );
  }

  private async syncActiveShiftToDexie(shift: CashShiftDto): Promise<void> {
    try {
      const entity: ActiveShiftEntity = {
        id: shift.id,
        branchId: shift.branchId,
        openedAt: shift.openedAt,
        status: shift.status,
        openedBy: shift.openedByName || shift.openedBy,
      };
      await this.db.activeShift.clear();
      await this.db.activeShift.put(entity);
    } catch {
      // Ignorar fallos de Dexie en entornos headless/SSR
    }
  }

  private async clearActiveShiftFromDexie(): Promise<void> {
    try {
      await this.db.activeShift.clear();
    } catch {
      // Ignorar fallos de Dexie en entornos headless/SSR
    }
  }
}
