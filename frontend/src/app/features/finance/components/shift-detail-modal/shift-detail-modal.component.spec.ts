import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ShiftDetailModalComponent } from './shift-detail-modal.component';
import { FinanceService } from '../../../../core/services/finance.service';
import { ShiftAuditDetailDto } from '../../../../core/models/finance.models';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';

describe('ShiftDetailModalComponent', () => {
  let component: ShiftDetailModalComponent;
  let fixture: ComponentFixture<ShiftDetailModalComponent>;
  let financeServiceSpy: {
    getShiftAuditDetail: ReturnType<typeof vi.fn>;
  };

  const mockAuditDetail: ShiftAuditDetailDto = {
    shift: {
      id: 'shift-1042',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      openedBy: 'user-1',
      openedByName: 'Cajero Juan',
      closedBy: 'admin-1',
      closedByName: 'Supervisor Carlos',
      openedAt: '2026-11-08T08:00:00Z',
      closedAt: '2026-11-08T16:00:00Z',
      initialCash: 200,
      totalCashSales: 500,
      totalCardSales: 300,
      totalOtherSales: 100,
      totalCashExpenses: 50,
      totalCashRefunds: 20,
      expectedCash: 630,
      actualCash: 630,
      difference: 0,
      status: 'CLOSED',
    },
    cashSales: 500,
    cardSales: 300,
    qrSales: 80,
    transferSales: 20,
    orders: [
      {
        id: 'order-1',
        ticketNumber: 'T-001',
        createdAt: '2026-11-08T09:30:00Z',
        customerName: 'Cliente A',
        paymentMethod: 'CASH',
        totalAmount: 120,
        orderStatus: 'PAID',
        orderType: 'DINE_IN',
      },
      {
        id: 'order-2',
        ticketNumber: 'T-002',
        createdAt: '2026-11-08T10:15:00Z',
        customerName: 'Cliente B',
        paymentMethod: 'CARD',
        totalAmount: 180,
        orderStatus: 'PAID',
        orderType: 'TAKEOUT',
      },
    ],
    expenses: [
      {
        id: 'exp-1',
        tenantId: 'tenant-1',
        branchId: 'branch-1',
        cashShiftId: 'shift-1042',
        expenseType: 'DAILY_OPERATIONAL',
        description: 'Compra de hielo',
        amount: 50,
        paidFromCashDrawer: true,
        approvalStatus: 'APPROVED',
        approvedBy: 'admin-1',
        approvedByName: 'Admin',
        expenseDate: '2026-11-08',
        createdBy: 'user-1',
        createdAt: '2026-11-08T11:00:00Z',
      },
    ],
    refunds: [
      {
        id: 'order-cancel-1',
        ticketNumber: 'T-099',
        createdAt: '2026-11-08T12:00:00Z',
        customerName: 'Cliente Retractado',
        paymentMethod: 'CASH',
        totalAmount: 20,
        orderStatus: 'CANCELLED',
        orderType: 'DINE_IN',
        cancellationReason: 'CUSTOMER_RETRACTED',
      },
    ],
  };

  beforeEach(async () => {
    financeServiceSpy = {
      getShiftAuditDetail: vi.fn().mockReturnValue(of(mockAuditDetail)),
    };

    await TestBed.configureTestingModule({
      imports: [ShiftDetailModalComponent],
      providers: [{ provide: FinanceService, useValue: financeServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ShiftDetailModalComponent);
    component = fixture.componentInstance;
  });

  it('no debe renderizar el modal cuando isOpen es false', () => {
    fixture.componentRef.setInput('isOpen', false);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.css('[data-testid="shift-detail-modal"]'));
    expect(modal).toBeNull();
  });

  it('debe cargar el detalle de auditoría al abrir el modal con shiftId', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    expect(financeServiceSpy.getShiftAuditDetail).toHaveBeenCalledWith('shift-1042');
    expect(component.detail()).toEqual(mockAuditDetail);

    const modal = fixture.debugElement.query(By.css('[data-testid="shift-detail-modal"]'));
    expect(modal).toBeTruthy();

    const cashSalesEl = fixture.debugElement.query(By.css('[data-testid="detail-cash-sales"]'));
    expect(cashSalesEl.nativeElement.textContent).toContain('500.00');

    const cardSalesEl = fixture.debugElement.query(By.css('[data-testid="detail-card-sales"]'));
    expect(cardSalesEl.nativeElement.textContent).toContain('300.00');

    const discrepancyBadge = fixture.debugElement.query(By.css('[data-testid="detail-discrepancy-badge"]'));
    expect(discrepancyBadge.nativeElement.textContent).toContain('CUADRADO');
  });

  it('debe alternar entre pestañas de Resumen, Comandas y Gastos', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    // Tab Comandas
    const ordersTabBtn = fixture.debugElement.query(By.css('[data-testid="tab-modal-orders"]'));
    ordersTabBtn.nativeElement.click();
    fixture.detectChanges();
    expect(component.activeTab()).toBe('orders');

    // Tab Gastos
    const expensesTabBtn = fixture.debugElement.query(By.css('[data-testid="tab-modal-expenses"]'));
    expensesTabBtn.nativeElement.click();
    fixture.detectChanges();
    expect(component.activeTab()).toBe('expenses');

    // Tab Resumen
    const summaryTabBtn = fixture.debugElement.query(By.css('[data-testid="tab-modal-summary"]'));
    summaryTabBtn.nativeElement.click();
    fixture.detectChanges();
    expect(component.activeTab()).toBe('summary');
  });

  it('debe invocar exportToCsv al hacer clic en Descargar Detalle (CSV)', () => {
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockReturnValue();

    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    const exportBtn = fixture.debugElement.query(By.css('[data-testid="btn-export-shift-detail"]'));
    expect(exportBtn).toBeTruthy();
    exportBtn.nativeElement.click();

    expect(clickSpy).toHaveBeenCalled();
    clickSpy.mockRestore();
  });

  it('debe emitir closeModal al hacer clic en el botón de cerrar', () => {
    const closeSpy = vi.spyOn(component.closeModal, 'emit');

    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    const closeBtn = fixture.debugElement.query(By.css('[data-testid="btn-close-modal"]'));
    expect(closeBtn).toBeTruthy();
    closeBtn.nativeElement.click();

    expect(closeSpy).toHaveBeenCalled();
  });

  it('debe mostrar mensaje de error si falla la llamada al servicio', () => {
    financeServiceSpy.getShiftAuditDetail.mockReturnValue(throwError(() => new Error('Error de red')));

    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('shiftId', 'shift-1042');
    fixture.detectChanges();

    expect(component.errorMessage()).toBeTruthy();
  });
});
