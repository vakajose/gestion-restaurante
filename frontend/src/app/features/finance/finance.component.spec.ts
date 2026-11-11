import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { FinanceComponent } from './finance.component';
import { FinanceService } from '../../core/services/finance.service';
import { AuthService } from '../../core/services/auth.service';
import { CashShiftDto, ExpenseDto } from '../../core/models/finance.models';

describe('FinanceComponent', () => {
  let component: FinanceComponent;
  let fixture: ComponentFixture<FinanceComponent>;
  let financeService: FinanceService;

  const mockShift: CashShiftDto = {
    id: 'shift-1',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
    openedBy: 'user-1',
    openedByName: 'admin',
    openedAt: '2026-10-01T10:00:00Z',
    initialCash: 200,
    totalCashSales: 150,
    totalCardSales: 50,
    totalOtherSales: 20,
    totalCashExpenses: 25,
    expectedCash: 325,
    status: 'OPEN',
  };

  const mockExpenses: ExpenseDto[] = [
    {
      id: 'exp-1',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      cashShiftId: 'shift-1',
      expenseType: 'DAILY_OPERATIONAL',
      description: 'Compra de hielo',
      amount: 15,
      paidFromCashDrawer: true,
      approvalStatus: 'PENDING_APPROVAL',
      expenseDate: '2026-10-01',
      createdBy: 'cajero1',
      createdAt: '2026-10-01T11:00:00Z',
    },
    {
      id: 'exp-2',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      cashShiftId: 'shift-1',
      expenseType: 'DAILY_OPERATIONAL',
      description: 'Bolsas y servilletas',
      amount: 25,
      paidFromCashDrawer: true,
      approvalStatus: 'APPROVED',
      expenseDate: '2026-10-01',
      createdBy: 'admin',
      createdAt: '2026-10-01T09:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinanceComponent],
      providers: [
        provideRouter([]),
        FinanceService,
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    financeService = TestBed.inject(FinanceService);

    // Mock HTTP methods with synchronous observable returns
    financeService.loadCurrentShift = () => {
      financeService.currentShift.set(mockShift);
      return of(mockShift);
    };
    financeService.loadShifts = () => {
      financeService.shifts.set([mockShift]);
      return of([mockShift]);
    };
    financeService.loadShiftsHistory = () => {
      financeService.shifts.set([mockShift]);
      return of([mockShift]);
    };
    financeService.loadExpenses = () => {
      financeService.expenses.set(mockExpenses);
      return of(mockExpenses);
    };
    financeService.loadAuthorizations = () => {
      financeService.authorizations.set([]);
      return of([]);
    };

    fixture = TestBed.createComponent(FinanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar datos', () => {
    expect(component).toBeTruthy();
    expect(financeService.currentShift()).toEqual(mockShift);
    expect(financeService.hasOpenShift()).toBe(true);
    expect(financeService.expenses().length).toBe(2);
  });

  it('debe alternar entre pestañas de turnos y gastos', () => {
    expect(component.activeTab()).toBe('shifts');
    component.activeTab.set('expenses');
    expect(component.activeTab()).toBe('expenses');
    expect(component.filteredExpenses().length).toBe(2);
  });

  it('debe filtrar gastos por estado correctamente', () => {
    component.activeTab.set('expenses');
    component.expenseFilter.set('PENDING_APPROVAL');
    expect(component.filteredExpenses().length).toBe(1);
    expect(component.filteredExpenses()[0].id).toBe('exp-1');

    component.expenseFilter.set('APPROVED');
    expect(component.filteredExpenses().length).toBe(1);
    expect(component.filteredExpenses()[0].id).toBe('exp-2');
  });

  it('debe calcular la diferencia del arqueo en vivo', () => {
    // Expected = 325
    component.actualCashInput.set(325);
    expect(component.liveDifference()).toBe(0);

    component.actualCashInput.set(330);
    expect(component.liveDifference()).toBe(5); // Sobrante

    component.actualCashInput.set(320);
    expect(component.liveDifference()).toBe(-5); // Faltante
  });

  it('debe abrir y cerrar modal de cierre de caja', () => {
    component.openCloseShiftModal();
    expect(component.showCloseShiftModal()).toBe(true);
    expect(component.actualCashInput()).toBe(325);

    component.closeCloseShiftModal();
    expect(component.showCloseShiftModal()).toBe(false);
  });

  it('debe abrir y cerrar modal de apertura de turno', () => {
    component.openOpenShiftModal();
    expect(component.showOpenShiftModal()).toBe(true);

    component.closeOpenShiftModal();
    expect(component.showOpenShiftModal()).toBe(false);
  });

  it('debe alternar a pestaña de autorizaciones remotas', () => {
    component.activeTab.set('authorizations');
    expect(component.activeTab()).toBe('authorizations');
  });

  it('debe invocar financeService.approveAuthorization al aprobar solicitud', () => {
    const approveSpy = vi.spyOn(financeService, 'approveAuthorization').mockReturnValue(
      of({
        id: 'auth-1',
        tenantId: 'tenant-1',
        branchId: 'branch-1',
        requestType: 'ORDER_CANCELLATION',
        referenceId: 'ord-1',
        requestedBy: 'user-1',
        amount: 150,
        status: 'APPROVED',
        createdAt: '2026-11-08T01:00:00Z',
      })
    );

    component.onApproveAuthorization({ id: 'auth-1', notes: 'Aprobado ok' });

    expect(approveSpy).toHaveBeenCalledWith('auth-1', 'Aprobado ok');
    expect(component.feedbackMessage()?.type).toBe('success');
  });

  it('debe invocar financeService.rejectAuthorization al rechazar solicitud', () => {
    const rejectSpy = vi.spyOn(financeService, 'rejectAuthorization').mockReturnValue(
      of({
        id: 'auth-1',
        tenantId: 'tenant-1',
        branchId: 'branch-1',
        requestType: 'ORDER_CANCELLATION',
        referenceId: 'ord-1',
        requestedBy: 'user-1',
        amount: 150,
        status: 'REJECTED',
        createdAt: '2026-11-08T01:00:00Z',
      })
    );

    component.onRejectAuthorization({ id: 'auth-1', reason: 'Rechazado por política' });

    expect(rejectSpy).toHaveBeenCalledWith('auth-1', 'Rechazado por política');
    expect(component.feedbackMessage()?.type).toBe('success');
  });

  it('debe aplicar presets de rango de fechas correctamente', () => {
    const historySpy = vi.spyOn(financeService, 'loadShiftsHistory');

    component.setPresetDateRange('today');
    expect(component.filterStartDate()).toBeTruthy();
    expect(component.filterEndDate()).toBe(component.filterStartDate());
    expect(historySpy).toHaveBeenCalled();

    component.setPresetDateRange('last7days');
    expect(component.filterStartDate()).not.toBe(component.filterEndDate());

    component.setPresetDateRange('thisMonth');
    expect(component.filterStartDate().endsWith('-01')).toBe(true);
  });

  it('debe invocar loadShiftsHistory con los filtros seleccionados', () => {
    const historySpy = vi.spyOn(financeService, 'loadShiftsHistory');

    component.filterStartDate.set('2026-10-01');
    component.filterEndDate.set('2026-10-15');
    component.filterCashierId.set('user-1');
    component.filterDiscrepancy.set('FALTANTE');
    component.filterShiftStatus.set('CLOSED');

    component.applyShiftFilters();

    expect(historySpy).toHaveBeenCalledWith(
      expect.objectContaining({
        startDate: '2026-10-01',
        endDate: '2026-10-15',
        cashierId: 'user-1',
        discrepancyType: 'FALTANTE',
        status: 'CLOSED',
        limit: 100,
      })
    );
  });

  it('debe abrir y cerrar el modal de detalle exhaustivo de turno', () => {
    expect(component.isDetailModalOpen()).toBe(false);
    expect(component.selectedShiftForDetail()).toBeNull();

    component.openShiftDetail('shift-123');
    expect(component.isDetailModalOpen()).toBe(true);
    expect(component.selectedShiftForDetail()).toBe('shift-123');

    component.closeShiftDetail();
    expect(component.isDetailModalOpen()).toBe(false);
    expect(component.selectedShiftForDetail()).toBeNull();
  });

  it('debe exportar la lista de turnos a CSV', () => {
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockReturnValue();

    component.exportShiftsListCsv();

    expect(clickSpy).toHaveBeenCalled();
    clickSpy.mockRestore();
  });

  it('debe limpiar los filtros al invocar clearShiftFilters', () => {
    const historySpy = vi.spyOn(financeService, 'loadShiftsHistory');

    component.filterStartDate.set('2026-10-01');
    component.filterEndDate.set('2026-10-15');
    component.filterCashierId.set('user-1');
    component.filterDiscrepancy.set('SOBRANTE');
    component.filterShiftStatus.set('CLOSED');
    component.searchShiftQuery.set('cajero');

    component.clearShiftFilters();

    expect(component.filterStartDate()).toBe('');
    expect(component.filterEndDate()).toBe('');
    expect(component.filterCashierId()).toBe('ALL');
    expect(component.filterDiscrepancy()).toBe('ALL');
    expect(component.filterShiftStatus()).toBe('ALL');
    expect(component.searchShiftQuery()).toBe('');
    expect(historySpy).toHaveBeenCalled();
  });
});
