import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FinanceService } from './finance.service';
import { AuthService } from './auth.service';
import { CashShiftDto, ExpenseDto } from '../models/finance.models';

describe('FinanceService', () => {
  let service: FinanceService;
  let httpMock: HttpTestingController;

  const mockShift: CashShiftDto = {
    id: 'shift-123',
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

  const mockExpense: ExpenseDto = {
    id: 'exp-1',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
    cashShiftId: 'shift-123',
    expenseType: 'DAILY_OPERATIONAL',
    description: 'Servilletas',
    amount: 25,
    paidFromCashDrawer: true,
    approvalStatus: 'PENDING_APPROVAL',
    expenseDate: '2026-10-01',
    createdBy: 'user-1',
    createdAt: '2026-10-01T11:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FinanceService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            currentUser: () => ({ branchId: 'branch-1', id: 'user-1', role: 'ADMIN_TENANT' }),
          },
        },
      ],
    });

    service = TestBed.inject(FinanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
    expect(service.currentShift()).toBeNull();
    expect(service.hasOpenShift()).toBe(false);
  });

  it('debe cargar el turno actual vía GET /shifts/current', () => {
    service.loadCurrentShift('branch-1').subscribe((shift) => {
      expect(shift).toEqual(mockShift);
      expect(service.currentShift()).toEqual(mockShift);
      expect(service.hasOpenShift()).toBe(true);
      expect(service.expectedDrawerCash()).toBe(325);
    });

    const req = httpMock.expectOne('/api/v1/finance/shifts/current?branchId=branch-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockShift);
  });

  it('debe abrir un nuevo turno vía POST /shifts/open', () => {
    service.openShift({ branchId: 'branch-1', initialCash: 200 }).subscribe((shift) => {
      expect(shift.id).toBe('shift-123');
      expect(service.currentShift()?.id).toBe('shift-123');
    });

    const req = httpMock.expectOne('/api/v1/finance/shifts/open');
    expect(req.request.method).toBe('POST');
    req.flush(mockShift);
  });

  it('debe cerrar el turno vía POST /shifts/{id}/close', () => {
    service.currentShift.set(mockShift);

    const closed = { ...mockShift, status: 'CLOSED' as const, actualCash: 325, difference: 0 };
    service.closeShift('shift-123', { actualCash: 325 }).subscribe((shift) => {
      expect(shift.status).toBe('CLOSED');
      expect(service.currentShift()).toBeNull();
      expect(service.hasOpenShift()).toBe(false);
    });

    const req = httpMock.expectOne('/api/v1/finance/shifts/shift-123/close');
    expect(req.request.method).toBe('POST');
    req.flush(closed);
  });

  it('debe registrar y aprobar un gasto de caja chica', () => {
    service.createExpense({
      branchId: 'branch-1',
      expenseType: 'DAILY_OPERATIONAL',
      description: 'Servilletas',
      amount: 25,
      paidFromCashDrawer: true,
    }).subscribe((exp) => {
      expect(exp.description).toBe('Servilletas');
      expect(service.expenses().length).toBe(1);
      expect(service.pendingApprovalCount()).toBe(1);
    });

    const reqCreate = httpMock.expectOne('/api/v1/finance/expenses');
    expect(reqCreate.request.method).toBe('POST');
    reqCreate.flush(mockExpense);

    // Aprobar gasto
    const approved = { ...mockExpense, approvalStatus: 'APPROVED' as const };
    service.approveExpense('exp-1', { status: 'APPROVED' }).subscribe((exp) => {
      expect(exp.approvalStatus).toBe('APPROVED');
      expect(service.pendingApprovalCount()).toBe(0);
      expect(service.totalApprovedExpenses()).toBe(25);
    });

    const reqApprove = httpMock.expectOne('/api/v1/finance/expenses/exp-1/approve');
    expect(reqApprove.request.method).toBe('POST');
    reqApprove.flush(approved);
  });
});
