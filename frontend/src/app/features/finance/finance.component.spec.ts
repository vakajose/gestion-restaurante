import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach } from 'vitest';
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
    financeService.loadExpenses = () => {
      financeService.expenses.set(mockExpenses);
      return of(mockExpenses);
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
});
