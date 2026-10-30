import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, OnDestroy, computed, inject, signal, DestroyRef, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { FinanceService } from '../../core/services/finance.service';
import { NotificationService } from '../../core/services/notification.service';
import { CashShiftDto, ExpenseDto, ExpenseType } from '../../core/models/finance.models';
import { OrderDto } from '../../core/models/pos.models';

import {
  CardComponent,
  ButtonComponent,
  BadgeComponent,
  ModalComponent,
  FormFieldComponent,
  IconComponent,
} from '../../shared/components/ui';

@Component({
  selector: 'app-cash-control',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    DecimalPipe,
    DatePipe,
    CardComponent,
    ButtonComponent,
    BadgeComponent,
    ModalComponent,
    FormFieldComponent,
    IconComponent,
  ],
  templateUrl: './cash-control.component.html',
})
export class CashControlComponent implements OnInit, OnDestroy {
  readonly financeService = inject(FinanceService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);
  readonly notifService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly currentBranchId = computed(() =>
    this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || undefined
  );

  // Navegación de pestañas
  readonly activeTab = signal<'orders' | 'expenses'>('orders');

  // Turno seleccionado (para multi-cajero)
  readonly selectedShiftId = signal<string | null>(null);

  // Filtros de Comandas
  readonly filterCashier = signal<string>('ALL'); // 'ALL' | 'MINE' | cashierId
  readonly filterPaymentMethod = signal<string>('ALL'); // 'ALL' | 'CASH' | 'CARD' | 'QR' | 'TRANSFER'
  readonly searchQuery = signal<string>('');

  // Filtros de Gastos
  readonly expenseFilter = signal<string>('ALL'); // 'ALL' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED'

  // Modal: Detalle de Comanda
  readonly isDetailModalOpen = signal<boolean>(false);
  readonly selectedOrder = signal<OrderDto | null>(null);

  // Estados de Formulario / Acciones
  readonly isSubmitting = signal<boolean>(false);

  // Modal: Abrir Turno
  readonly showOpenShiftModal = signal<boolean>(false);
  readonly initialCashInput = signal<number>(200);

  // Modal: Cerrar Turno / Arqueo
  readonly showCloseShiftModal = signal<boolean>(false);
  readonly actualCashInput = signal<number>(0);
  readonly closingNotes = signal<string>('');

  // Modal: Nuevo Gasto Menor
  readonly showNewExpenseModal = signal<boolean>(false);
  readonly expenseDesc = signal<string>('');
  readonly expenseAmount = signal<number>(0);
  readonly expenseType = signal<ExpenseType>('DAILY_OPERATIONAL');
  readonly expensePaidDrawer = signal<boolean>(true);
  readonly expenseEvidence = signal<string>('');
  readonly expenseDate = signal<string>(new Date().toISOString().substring(0, 10));

  private autoRefreshSub?: Subscription;

  // Permisos y Usuario
  readonly currentUser = computed(() => this.authService.currentUser());

  readonly canApprove = computed(() => {
    const role = this.currentUser()?.role || '';
    return ['ADMIN_TENANT', 'BRANCH_MANAGER'].includes(role);
  });

  // Turno Activo Efectivo (prioriza el seleccionado o el currentShift del usuario)
  readonly activeShift = computed<CashShiftDto | null>(() => {
    const selId = this.selectedShiftId();
    if (selId) {
      const found = this.financeService.activeShifts().find((s) => s.id === selId);
      if (found) return found;
    }
    return this.financeService.currentShift();
  });

  readonly hasActiveShift = computed<boolean>(() => {
    const shift = this.activeShift();
    return Boolean(shift && shift.status === 'OPEN');
  });

  // Diferencia calculada para el arqueo ciego
  readonly liveDifference = computed<number>(() => {
    const expected = this.financeService.expectedDrawerCash();
    const actual = this.actualCashInput() || 0;
    return Number((actual - expected).toFixed(2));
  });

  // Lista de comandas filtradas reactivamente
  readonly filteredOrders = computed<OrderDto[]>(() => {
    let list = this.financeService.shiftOrders();
    const cashier = this.filterCashier();
    const currentUserId = this.currentUser()?.id;
    const currentUsername = this.currentUser()?.username;

    if (cashier === 'MINE') {
      list = list.filter(
        (o) => (currentUserId && o.cashierId === currentUserId) || (currentUsername && o.cashierId === currentUsername)
      );
    } else if (cashier !== 'ALL') {
      list = list.filter((o) => o.cashierId === cashier);
    }

    const method = this.filterPaymentMethod();
    if (method !== 'ALL') {
      list = list.filter((o) => o.paymentMethod?.toUpperCase() === method.toUpperCase());
    }

    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      list = list.filter(
        (o) =>
          o.ticketNumber?.toLowerCase().includes(query) ||
          o.id?.toLowerCase().includes(query) ||
          (o.notes && o.notes.toLowerCase().includes(query))
      );
    }

    return list;
  });

  // Lista de gastos filtrados
  readonly filteredExpenses = computed<ExpenseDto[]>(() => {
    const list = this.financeService.expenses();
    const filter = this.expenseFilter();
    if (filter === 'ALL') {
      return list;
    }
    return list.filter((e) => e.approvalStatus === filter);
  });

  // Opciones únicas de cajeros que tienen ventas o turnos
  readonly availableCashiers = computed<string[]>(() => {
    const set = new Set<string>();
    for (const ord of this.financeService.shiftOrders()) {
      if (ord.cashierId) set.add(ord.cashierId);
    }
    for (const sh of this.financeService.activeShifts()) {
      if (sh.openedByName) set.add(sh.openedByName);
    }
    return Array.from(set);
  });

  constructor() {
    effect(() => {
      const branchId = this.currentBranchId();
      if (branchId) {
        this.refreshAll();
      }
    });
  }

  ngOnInit(): void {
    this.refreshAll();

    // Auto-refresh silencioso cada 30 segundos
    this.autoRefreshSub = interval(30000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.silentRefresh();
      });
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
  }

  /**
   * Refresco completo de datos.
   */
  refreshAll(): void {
    const branchId = this.currentBranchId();
    if (!branchId) return;

    this.financeService.loadCurrentShift(branchId).subscribe((shift) => {
      if (shift && !this.selectedShiftId()) {
        this.selectedShiftId.set(shift.id);
      }
      this.loadShiftOrders(this.activeShift()?.id);
    });

    this.financeService.loadActiveShifts(branchId).subscribe();
    this.financeService.loadExpenses(branchId).subscribe();
  }

  /**
   * Refresco silencioso periódico sin spinner de pantalla completa.
   */
  silentRefresh(): void {
    const branchId = this.currentBranchId();
    if (!branchId) return;

    this.financeService.loadCurrentShift(branchId).subscribe(() => {
      this.loadShiftOrders(this.activeShift()?.id);
    });
    this.financeService.loadActiveShifts(branchId).subscribe();
    this.financeService.loadExpenses(branchId).subscribe();
  }

  /**
   * Cambia el turno enfocado (para vista multi-cajero).
   */
  selectShift(shiftId: string): void {
    this.selectedShiftId.set(shiftId);
    this.loadShiftOrders(shiftId);
  }

  /**
   * Carga las órdenes correspondientes a un turno específico.
   */
  loadShiftOrders(shiftId?: string): void {
    const targetShiftId = shiftId || this.activeShift()?.id;
    if (!targetShiftId) {
      this.financeService.shiftOrders.set([]);
      return;
    }
    this.financeService.loadShiftOrders(targetShiftId).subscribe();
  }

  // --- Acciones de Apertura y Cierre ---

  openOpenShiftModal(): void {
    this.initialCashInput.set(200);
    this.showOpenShiftModal.set(true);
  }

  closeOpenShiftModal(): void {
    this.showOpenShiftModal.set(false);
  }

  confirmOpenShift(): void {
    const branchId = this.currentUser()?.branchId;
    if (!branchId) {
      this.notifService.error('No hay una sucursal activa asignada.');
      return;
    }

    this.isSubmitting.set(true);
    this.financeService.openShift({ branchId, initialCash: this.initialCashInput() }).subscribe({
      next: (newShift) => {
        this.isSubmitting.set(false);
        this.showOpenShiftModal.set(false);
        this.selectedShiftId.set(newShift.id);
        this.notifService.success('Turno de caja abierto correctamente.');
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al abrir el turno de caja.';
        this.notifService.error(msg);
      },
    });
  }

  openCloseShiftModal(): void {
    const expected = this.financeService.expectedDrawerCash();
    this.actualCashInput.set(expected);
    this.closingNotes.set('');
    this.showCloseShiftModal.set(true);
  }

  closeCloseShiftModal(): void {
    this.showCloseShiftModal.set(false);
  }

  confirmCloseShift(): void {
    const shift = this.activeShift();
    if (!shift) return;

    this.isSubmitting.set(true);
    this.financeService
      .closeShift(shift.id, {
        actualCash: this.actualCashInput(),
        notes: this.closingNotes(),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.showCloseShiftModal.set(false);
          this.selectedShiftId.set(null);
          this.notifService.success('Turno cerrado y arqueo asentado con éxito.');
          this.refreshAll();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al cerrar el turno.';
          this.notifService.error(msg);
        },
      });
  }

  // --- Modal de Detalle de Comanda ---

  viewOrderDetail(order: OrderDto): void {
    this.selectedOrder.set(order);
    this.isDetailModalOpen.set(true);
  }

  closeDetailModal(): void {
    this.isDetailModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  printOrder(order: OrderDto): void {
    this.notifService.info(`Enviando a impresión ticket #${order.ticketNumber}...`);
    window.print();
  }

  // --- Acciones de Gastos Menores ---

  openNewExpenseModal(): void {
    this.expenseDesc.set('');
    this.expenseAmount.set(0);
    this.expenseType.set('DAILY_OPERATIONAL');
    this.expensePaidDrawer.set(true);
    this.expenseEvidence.set('');
    this.expenseDate.set(new Date().toISOString().substring(0, 10));
    this.showNewExpenseModal.set(true);
  }

  closeNewExpenseModal(): void {
    this.showNewExpenseModal.set(false);
  }

  confirmNewExpense(): void {
    const branchId = this.currentUser()?.branchId;
    if (!branchId) return;

    if (!this.expenseDesc().trim() || this.expenseAmount() <= 0) {
      this.notifService.warning('Complete la descripción y un monto mayor a 0.');
      return;
    }

    this.isSubmitting.set(true);
    this.financeService
      .createExpense({
        branchId,
        cashShiftId: this.activeShift()?.id,
        expenseType: this.expenseType(),
        description: this.expenseDesc().trim(),
        amount: this.expenseAmount(),
        paidFromCashDrawer: this.expensePaidDrawer(),
        evidenceUrl: this.expenseEvidence().trim() || undefined,
        expenseDate: this.expenseDate(),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.showNewExpenseModal.set(false);
          this.notifService.success('Gasto registrado correctamente.');
          this.refreshAll();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al registrar el gasto.';
          this.notifService.error(msg);
        },
      });
  }

  approveExpense(expense: ExpenseDto): void {
    this.isSubmitting.set(true);
    this.financeService
      .approveExpense(expense.id, {
        status: 'APPROVED',
        approvalNotes: 'Aprobado por administración de sucursal',
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.notifService.success(`Gasto "${expense.description}" aprobado.`);
          this.refreshAll();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.detail || 'Error al aprobar el gasto.';
          this.notifService.error(msg);
        },
      });
  }

  rejectExpense(expense: ExpenseDto): void {
    this.isSubmitting.set(true);
    this.financeService
      .approveExpense(expense.id, {
        status: 'REJECTED',
        approvalNotes: 'Gasto rechazado en revisión de turno',
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.notifService.success(`Gasto "${expense.description}" rechazado.`);
          this.refreshAll();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.detail || 'Error al rechazar el gasto.';
          this.notifService.error(msg);
        },
      });
  }
}
