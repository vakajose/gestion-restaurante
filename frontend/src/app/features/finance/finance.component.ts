import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { FinanceService } from '../../core/services/finance.service';
import { ExpenseDto, ExpenseType } from '../../core/models/finance.models';

@Component({
  selector: 'app-finance',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './finance.component.html',
})
export class FinanceComponent implements OnInit {
  readonly financeService = inject(FinanceService);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<'shifts' | 'expenses'>('shifts');
  readonly expenseFilter = signal<string>('ALL');
  readonly isSubmitting = signal<boolean>(false);
  readonly feedbackMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  // Modal: Abrir Turno
  readonly showOpenShiftModal = signal<boolean>(false);
  readonly initialCashInput = signal<number>(200);

  // Modal: Cerrar Turno / Arqueo
  readonly showCloseShiftModal = signal<boolean>(false);
  readonly actualCashInput = signal<number>(0);
  readonly closingNotes = signal<string>('');

  // Modal: Nuevo Gasto
  readonly showNewExpenseModal = signal<boolean>(false);
  readonly expenseDesc = signal<string>('');
  readonly expenseAmount = signal<number>(0);
  readonly expenseType = signal<ExpenseType>('DAILY_OPERATIONAL');
  readonly expensePaidDrawer = signal<boolean>(true);
  readonly expenseEvidence = signal<string>('');
  readonly expenseDate = signal<string>(new Date().toISOString().substring(0, 10));

  // Permisos de usuario
  readonly canApprove = computed(() => {
    const role = this.authService.currentUser()?.role || '';
    return ['ADMIN_TENANT', 'BRANCH_MANAGER'].includes(role);
  });

  // Arqueo en vivo en modal
  readonly liveDifference = computed(() => {
    const expected = this.financeService.expectedDrawerCash();
    const actual = this.actualCashInput() || 0;
    return Number((actual - expected).toFixed(2));
  });

  // Gastos filtrados
  readonly filteredExpenses = computed(() => {
    const list = this.financeService.expenses();
    const filter = this.expenseFilter();
    if (filter === 'ALL') {
      return list;
    }
    return list.filter((e) => e.approvalStatus === filter);
  });

  ngOnInit(): void {
    this.refreshAll();
  }

  refreshAll(): void {
    const branchId = this.authService.currentUser()?.branchId || undefined;
    this.financeService.loadCurrentShift(branchId).subscribe();
    this.financeService.loadShifts(branchId).subscribe();
    this.financeService.loadExpenses(branchId).subscribe();
  }

  // --- Acciones de Turno ---

  openOpenShiftModal(): void {
    this.initialCashInput.set(200);
    this.feedbackMessage.set(null);
    this.showOpenShiftModal.set(true);
  }

  closeOpenShiftModal(): void {
    this.showOpenShiftModal.set(false);
  }

  confirmOpenShift(): void {
    const branchId = this.authService.currentUser()?.branchId;
    if (!branchId) {
      this.feedbackMessage.set({ type: 'error', text: 'No hay una sucursal activa asignada.' });
      return;
    }

    this.isSubmitting.set(true);
    this.financeService.openShift({ branchId, initialCash: this.initialCashInput() }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showOpenShiftModal.set(false);
        this.feedbackMessage.set({ type: 'success', text: 'Turno de caja abierto exitosamente.' });
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al abrir el turno.';
        this.feedbackMessage.set({ type: 'error', text: msg });
      },
    });
  }

  openCloseShiftModal(): void {
    const expected = this.financeService.expectedDrawerCash();
    this.actualCashInput.set(expected);
    this.closingNotes.set('');
    this.feedbackMessage.set(null);
    this.showCloseShiftModal.set(true);
  }

  closeCloseShiftModal(): void {
    this.showCloseShiftModal.set(false);
  }

  confirmCloseShift(): void {
    const shift = this.financeService.currentShift();
    if (!shift) return;

    this.isSubmitting.set(true);
    this.financeService.closeShift(shift.id, {
      actualCash: this.actualCashInput(),
      notes: this.closingNotes(),
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showCloseShiftModal.set(false);
        this.feedbackMessage.set({ type: 'success', text: 'Turno cerrado y arqueo asentado con éxito.' });
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al cerrar el turno.';
        this.feedbackMessage.set({ type: 'error', text: msg });
      },
    });
  }

  // --- Acciones de Gastos ---

  openNewExpenseModal(): void {
    this.expenseDesc.set('');
    this.expenseAmount.set(0);
    this.expenseType.set('DAILY_OPERATIONAL');
    this.expensePaidDrawer.set(true);
    this.expenseEvidence.set('');
    this.expenseDate.set(new Date().toISOString().substring(0, 10));
    this.feedbackMessage.set(null);
    this.showNewExpenseModal.set(true);
  }

  closeNewExpenseModal(): void {
    this.showNewExpenseModal.set(false);
  }

  confirmNewExpense(): void {
    const branchId = this.authService.currentUser()?.branchId;
    if (!branchId) return;

    if (!this.expenseDesc().trim() || this.expenseAmount() <= 0) {
      this.feedbackMessage.set({ type: 'error', text: 'Complete los campos obligatorios del gasto.' });
      return;
    }

    this.isSubmitting.set(true);
    this.financeService.createExpense({
      branchId,
      cashShiftId: this.financeService.currentShift()?.id,
      expenseType: this.expenseType(),
      description: this.expenseDesc().trim(),
      amount: this.expenseAmount(),
      paidFromCashDrawer: this.expensePaidDrawer(),
      evidenceUrl: this.expenseEvidence().trim() || undefined,
      expenseDate: this.expenseDate(),
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showNewExpenseModal.set(false);
        this.feedbackMessage.set({ type: 'success', text: 'Gasto registrado correctamente.' });
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al registrar el gasto.';
        this.feedbackMessage.set({ type: 'error', text: msg });
      },
    });
  }

  approveExpense(expense: ExpenseDto): void {
    this.isSubmitting.set(true);
    this.financeService.approveExpense(expense.id, {
      status: 'APPROVED',
      approvalNotes: 'Aprobado por administración de sucursal',
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.feedbackMessage.set({ type: 'success', text: `Gasto "${expense.description}" aprobado.` });
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || 'Error al aprobar el gasto.';
        this.feedbackMessage.set({ type: 'error', text: msg });
      },
    });
  }

  rejectExpense(expense: ExpenseDto): void {
    this.isSubmitting.set(true);
    this.financeService.approveExpense(expense.id, {
      status: 'REJECTED',
      approvalNotes: 'Gasto rechazado en revisión administrativa',
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.feedbackMessage.set({ type: 'success', text: `Gasto "${expense.description}" rechazado.` });
        this.refreshAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.detail || 'Error al rechazar el gasto.';
        this.feedbackMessage.set({ type: 'error', text: msg });
      },
    });
  }
}
