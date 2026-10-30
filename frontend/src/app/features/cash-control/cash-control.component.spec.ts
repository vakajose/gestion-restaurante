import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach } from 'vitest';
import { of } from 'rxjs';

import { CashControlComponent } from './cash-control.component';
import { FinanceService } from '../../core/services/finance.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { CashShiftDto, ExpenseDto } from '../../core/models/finance.models';
import { OrderDto } from '../../core/models/pos.models';

describe('CashControlComponent', () => {
  let component: CashControlComponent;
  let fixture: ComponentFixture<CashControlComponent>;
  let financeService: FinanceService;

  const mockShift: CashShiftDto = {
    id: 'shift-100',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
    openedBy: 'cajero-1',
    openedByName: 'cajero1',
    openedAt: '2026-10-30T10:00:00Z',
    initialCash: 200,
    totalCashSales: 350,
    totalCardSales: 120,
    totalOtherSales: 80,
    totalCashExpenses: 50,
    expectedCash: 500, // 200 + 350 - 50 = 500
    status: 'OPEN',
  };

  const mockOrders: OrderDto[] = [
    {
      id: 'ord-1',
      branchId: 'branch-1',
      cashShiftId: 'shift-100',
      cashierId: 'cajero1',
      ticketNumber: '1001',
      orderStatus: 'COMPLETED',
      paymentMethod: 'CASH',
      totalAmount: 150,
      createdAt: '2026-10-30T10:15:00Z',
      items: [
        {
          dishId: 'd-1',
          dishName: 'Hamburguesa Doble',
          quantity: 2,
          unitPrice: 75,
          subtotal: 150,
        },
      ],
    },
    {
      id: 'ord-2',
      branchId: 'branch-1',
      cashShiftId: 'shift-100',
      cashierId: 'cajero2',
      ticketNumber: '1002',
      orderStatus: 'COMPLETED',
      paymentMethod: 'CARD',
      totalAmount: 120,
      createdAt: '2026-10-30T10:30:00Z',
      items: [
        {
          dishId: 'd-2',
          dishName: 'Pizza Mediana',
          quantity: 1,
          unitPrice: 120,
          subtotal: 120,
        },
      ],
    },
    {
      id: 'ord-3',
      branchId: 'branch-1',
      cashShiftId: 'shift-100',
      cashierId: 'cajero1',
      ticketNumber: '1003',
      orderStatus: 'COMPLETED',
      paymentMethod: 'QR',
      totalAmount: 80,
      createdAt: '2026-10-30T11:00:00Z',
      items: [
        {
          dishId: 'd-3',
          dishName: 'Refresco y Porción de Papas',
          quantity: 2,
          unitPrice: 40,
          subtotal: 80,
        },
      ],
    },
  ];

  const mockExpenses: ExpenseDto[] = [
    {
      id: 'exp-1',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      cashShiftId: 'shift-100',
      expenseType: 'DAILY_OPERATIONAL',
      description: 'Hielo bolsa grande',
      amount: 20,
      paidFromCashDrawer: true,
      approvalStatus: 'PENDING_APPROVAL',
      expenseDate: '2026-10-30',
      createdBy: 'cajero1',
      createdAt: '2026-10-30T10:45:00Z',
    },
    {
      id: 'exp-2',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      cashShiftId: 'shift-100',
      expenseType: 'DAILY_OPERATIONAL',
      description: 'Servilletas y cubiertos',
      amount: 30,
      paidFromCashDrawer: true,
      approvalStatus: 'APPROVED',
      expenseDate: '2026-10-30',
      createdBy: 'admin',
      createdAt: '2026-10-30T10:20:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CashControlComponent],
      providers: [
        provideRouter([]),
        FinanceService,
        AuthService,
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({
      id: 'cajero-1',
      username: 'cajero1',
      email: 'cajero1@restaurant.com',
      role: 'CASHIER',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
    });

    financeService = TestBed.inject(FinanceService);

    // Mock métodos reactivos de FinanceService
    financeService.loadCurrentShift = () => {
      financeService.currentShift.set(mockShift);
      return of(mockShift);
    };
    financeService.loadActiveShifts = () => {
      financeService.activeShifts.set([mockShift]);
      return of([mockShift]);
    };
    financeService.loadExpenses = () => {
      financeService.expenses.set(mockExpenses);
      return of(mockExpenses);
    };
    financeService.loadShiftOrders = () => {
      financeService.shiftOrders.set(mockOrders);
      return of(mockOrders);
    };

    fixture = TestBed.createComponent(CashControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar el turno activo y comandas', () => {
    expect(component).toBeTruthy();
    expect(component.hasActiveShift()).toBe(true);
    expect(component.filteredOrders().length).toBe(3);
    expect(financeService.expectedDrawerCash()).toBe(500);
  });

  it('debe filtrar comandas por cajero', () => {
    component.filterCashier.set('cajero1');
    expect(component.filteredOrders().length).toBe(2);

    component.filterCashier.set('cajero2');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].ticketNumber).toBe('1002');

    component.filterCashier.set('ALL');
    expect(component.filteredOrders().length).toBe(3);
  });

  it('debe filtrar comandas por método de pago', () => {
    component.filterPaymentMethod.set('CASH');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].ticketNumber).toBe('1001');

    component.filterPaymentMethod.set('CARD');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].ticketNumber).toBe('1002');

    component.filterPaymentMethod.set('QR');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].ticketNumber).toBe('1003');

    component.filterPaymentMethod.set('ALL');
    expect(component.filteredOrders().length).toBe(3);
  });

  it('debe filtrar comandas por búsqueda de ticket', () => {
    component.searchQuery.set('1003');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].id).toBe('ord-3');

    component.searchQuery.set('inexistente');
    expect(component.filteredOrders().length).toBe(0);
  });

  it('debe abrir y cerrar el modal de detalle de comanda', () => {
    expect(component.isDetailModalOpen()).toBe(false);
    expect(component.selectedOrder()).toBeNull();

    component.viewOrderDetail(mockOrders[0]);
    expect(component.isDetailModalOpen()).toBe(true);
    expect(component.selectedOrder()?.ticketNumber).toBe('1001');

    component.closeDetailModal();
    expect(component.isDetailModalOpen()).toBe(false);
    expect(component.selectedOrder()).toBeNull();
  });

  it('debe alternar entre pestañas y filtrar gastos', () => {
    component.activeTab.set('expenses');
    expect(component.activeTab()).toBe('expenses');

    component.expenseFilter.set('PENDING_APPROVAL');
    expect(component.filteredExpenses().length).toBe(1);
    expect(component.filteredExpenses()[0].id).toBe('exp-1');

    component.expenseFilter.set('APPROVED');
    expect(component.filteredExpenses().length).toBe(1);
    expect(component.filteredExpenses()[0].id).toBe('exp-2');
  });

  it('debe calcular la diferencia del recuento ciego en el cierre de turno', () => {
    // Expected drawer cash = 500
    component.actualCashInput.set(500);
    expect(component.liveDifference()).toBe(0);

    component.actualCashInput.set(520);
    expect(component.liveDifference()).toBe(20); // Sobrante

    component.actualCashInput.set(480);
    expect(component.liveDifference()).toBe(-20); // Faltante
  });
});
