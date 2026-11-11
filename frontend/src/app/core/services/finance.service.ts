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
  ShiftAuditDetailDto,
  ShiftAuthorizationDto,
  CreateShiftAuthorizationRequest,
  ValidatePinRequest,
  ValidatePinResponse,
} from '../models/finance.models';
import { OrderDto } from '../models/pos.models';
import { AuthService } from './auth.service';
import { LocalShiftRepository } from '../repositories/local-shift.repository';
import { ActiveShiftEntity } from '../models/pos.models';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly localShiftRepo = inject(LocalShiftRepository);
  private readonly baseUrl = '/api/v1/finance';

  // Estados Reactivos con Signals
  readonly currentShift = signal<CashShiftDto | null>(null);
  readonly activeShifts = signal<CashShiftDto[]>([]);
  readonly shiftOrders = signal<OrderDto[]>([]);
  readonly shifts = signal<CashShiftDto[]>([]);
  readonly expenses = signal<ExpenseDto[]>([]);
  readonly authorizations = signal<ShiftAuthorizationDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Estados Computados
  readonly hasOpenShift = computed(() => this.currentShift()?.status === 'OPEN');

  readonly pendingApprovalCount = computed(
    () => this.expenses().filter((e) => e.approvalStatus === 'PENDING_APPROVAL').length
  );

  readonly pendingAuthorizationCount = computed(
    () => this.authorizations().filter((a) => a.status === 'PENDING').length
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
    const refunds = shift.totalCashRefunds || 0;
    return Number((initial + sales - expenses - refunds).toFixed(2));
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
   * Consolida y arquea formalmente un turno de caja en estado CLOSED (Supervisor/Admin).
   */
  consolidateShift(shiftId: string, notes?: string): Observable<CashShiftDto> {
    this.isLoading.set(true);
    return this.http
      .post<CashShiftDto>(`${this.baseUrl}/shifts/${shiftId}/consolidate`, notes ? { notes } : {})
      .pipe(
        tap((shift) => {
          this.isLoading.set(false);
          this.shifts.update((current) =>
            current.map((s) => (s.id === shift.id ? shift : s))
          );
        }),
        catchError((err) => {
          this.isLoading.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al consolidar el turno';
          this.errorMessage.set(msg);
          throw err;
        })
      );
  }

  /**
   * Reabre un turno previamente consolidado a CLOSED para correcciones (Supervisor/Admin).
   */
  reopenShift(shiftId: string): Observable<CashShiftDto> {
    this.isLoading.set(true);
    return this.http.post<CashShiftDto>(`${this.baseUrl}/shifts/${shiftId}/reopen`, {}).pipe(
      tap((shift) => {
        this.isLoading.set(false);
        this.shifts.update((current) =>
          current.map((s) => (s.id === shift.id ? shift : s))
        );
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al reabrir el turno';
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
   * Carga el historial de turnos con filtros avanzados multicriterio (ADR-0023 / PB-030).
   */
  loadShiftsHistory(filters?: {
    branchId?: string;
    startDate?: string;
    endDate?: string;
    cashierId?: string;
    status?: string;
    discrepancyType?: string;
    limit?: number;
  }): Observable<CashShiftDto[]> {
    this.isLoading.set(true);
    let params = new HttpParams();
    if (filters?.branchId) params = params.set('branchId', filters.branchId);
    if (filters?.startDate) params = params.set('startDate', filters.startDate);
    if (filters?.endDate) params = params.set('endDate', filters.endDate);
    if (filters?.cashierId && filters.cashierId !== 'ALL') params = params.set('cashierId', filters.cashierId);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.discrepancyType) params = params.set('discrepancyType', filters.discrepancyType);
    if (filters?.limit) params = params.set('limit', filters.limit.toString());

    return this.http.get<CashShiftDto[]>(`${this.baseUrl}/shifts/history`, { params }).pipe(
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
   * Obtiene la auditoría en profundidad de un turno con órdenes, gastos y devoluciones (ADR-0023).
   */
  getShiftAuditDetail(shiftId: string): Observable<ShiftAuditDetailDto> {
    return this.http.get<ShiftAuditDetailDto>(`${this.baseUrl}/shifts/${shiftId}/audit-detail`);
  }

  /**
   * Carga todos los turnos abiertos concurrentes en la sucursal (para monitoreo multi-cajero).
   */
  loadActiveShifts(branchId?: string): Observable<CashShiftDto[]> {
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    if (!effectiveBranchId) {
      this.activeShifts.set([]);
      return of([]);
    }

    const params = new HttpParams().set('branchId', effectiveBranchId);
    return this.http.get<CashShiftDto[]>(`${this.baseUrl}/shifts/active`, { params }).pipe(
      tap((shifts) => this.activeShifts.set(shifts || [])),
      catchError(() => {
        this.activeShifts.set([]);
        return of([]);
      })
    );
  }

  /**
   * Carga las comandas y tickets de un turno en curso con filtros opcionales.
   */
  loadShiftOrders(
    shiftId?: string,
    cashierId?: string,
    paymentMethod?: string,
    branchId?: string
  ): Observable<OrderDto[]> {
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    let params = new HttpParams();
    if (effectiveBranchId) {
      params = params.set('branchId', effectiveBranchId);
    }
    if (shiftId) {
      params = params.set('cashShiftId', shiftId);
    }
    if (cashierId && cashierId !== 'ALL') {
      params = params.set('cashierId', cashierId);
    }

    return this.http.get<OrderDto[]>('/api/v1/pos/orders', { params }).pipe(
      tap((orders) => {
        let filtered = orders || [];
        if (paymentMethod && paymentMethod !== 'ALL') {
          filtered = filtered.filter((o) => o.paymentMethod?.toUpperCase() === paymentMethod.toUpperCase());
        }
        this.shiftOrders.set(filtered);
      }),
      catchError(() => {
        this.shiftOrders.set([]);
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

  /**
   * Carga las solicitudes de autorización de turno (filtro por sucursal y estado).
   */
  loadAuthorizations(branchId?: string, status?: string): Observable<ShiftAuthorizationDto[]> {
    this.isLoading.set(true);
    let params = new HttpParams();
    const effectiveBranchId = branchId || this.authService.currentUser()?.branchId;
    if (effectiveBranchId) {
      params = params.set('branchId', effectiveBranchId);
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ShiftAuthorizationDto[]>(`${this.baseUrl}/authorizations`, { params }).pipe(
      tap((list) => {
        this.authorizations.set(list || []);
        this.isLoading.set(false);
        this.errorMessage.set(null);
      }),
      catchError(() => {
        this.isLoading.set(false);
        this.errorMessage.set('Error al cargar autorizaciones de turno');
        return of([]);
      })
    );
  }

  /**
   * Crea una nueva solicitud de autorización remota de turno.
   */
  createAuthorization(request: CreateShiftAuthorizationRequest): Observable<ShiftAuthorizationDto> {
    this.isLoading.set(true);
    return this.http.post<ShiftAuthorizationDto>(`${this.baseUrl}/authorizations`, request).pipe(
      tap((created) => {
        this.authorizations.update((list) => [created, ...list]);
        this.isLoading.set(false);
        this.errorMessage.set(null);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al crear la solicitud de autorización';
        this.errorMessage.set(msg);
        throw err;
      })
    );
  }

  /**
   * Valida in-situ el PIN numérico de un supervisor/admin.
   */
  validateSupervisorPin(request: ValidatePinRequest): Observable<ValidatePinResponse> {
    return this.http.post<ValidatePinResponse>(`${this.baseUrl}/authorizations/validate-pin`, request);
  }

  /**
   * Aprueba una solicitud de autorización pendiente (canal SUPERVISOR).
   */
  approveAuthorization(id: string, notes?: string): Observable<ShiftAuthorizationDto> {
    return this.http.post<ShiftAuthorizationDto>(`${this.baseUrl}/authorizations/${id}/approve`, { notes }).pipe(
      tap((updated) => {
        this.authorizations.update((list) => list.map((a) => (a.id === updated.id ? updated : a)));
      })
    );
  }

  /**
   * Rechaza una solicitud de autorización pendiente (canal SUPERVISOR).
   */
  rejectAuthorization(id: string, notes?: string): Observable<ShiftAuthorizationDto> {
    return this.http.post<ShiftAuthorizationDto>(`${this.baseUrl}/authorizations/${id}/reject`, { notes }).pipe(
      tap((updated) => {
        this.authorizations.update((list) => list.map((a) => (a.id === updated.id ? updated : a)));
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
      await this.localShiftRepo.saveActiveShift(entity);
    } catch {
      // Ignorar fallos de Dexie en entornos headless/SSR
    }
  }

  private async clearActiveShiftFromDexie(): Promise<void> {
    try {
      await this.localShiftRepo.clearActiveShift();
    } catch {
      // Ignorar fallos de Dexie en entornos headless/SSR
    }
  }
}
