import { Component, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinanceService } from '../../../../core/services/finance.service';
import { ShiftAuditDetailDto } from '../../../../core/models/finance.models';
import { exportToCsv } from '../../../../shared/utils/csv-exporter';

export type ShiftDetailModalTab = 'summary' | 'orders' | 'expenses';

@Component({
  selector: 'app-shift-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shift-detail-modal.component.html',
})
export class ShiftDetailModalComponent {
  private readonly financeService = inject(FinanceService);

  readonly shiftId = input.required<string>();
  readonly isOpen = input<boolean>(false);
  readonly closeModal = output<void>();

  readonly activeTab = signal<ShiftDetailModalTab>('summary');
  readonly detail = signal<ShiftAuditDetailDto | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      const open = this.isOpen();
      const id = this.shiftId();
      if (open && id) {
        this.loadDetail(id);
      } else if (!open) {
        this.detail.set(null);
        this.errorMessage.set(null);
        this.activeTab.set('summary');
      }
    });
  }

  loadDetail(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.financeService.getShiftAuditDetail(id).subscribe({
      next: (data) => {
        this.detail.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Error al cargar la auditoría en profundidad del turno.');
      },
    });
  }

  setTab(tab: ShiftDetailModalTab): void {
    this.activeTab.set(tab);
  }

  exportDetailCsv(): void {
    const data = this.detail();
    if (!data) return;

    const shift = data.shift;
    const shiftRef = shift.id.substring(0, 8);
    const filename = `detalle_turno_${shiftRef}.csv`;

    const headers = [
      'Tipo de Registro',
      'Ticket / ID',
      'Fecha / Hora',
      'Cliente / Concepto',
      'Medio de Pago / Categoría',
      'Monto (Bs.)',
      'Estado',
    ];

    const rows: (string | number | undefined | null)[][] = [];

    // Comandas / Ventas
    for (const order of data.orders) {
      rows.push([
        'VENTA',
        order.ticketNumber || order.id,
        order.createdAt,
        order.customerName || 'Consumidor Final',
        order.paymentMethod,
        order.totalAmount,
        order.orderStatus,
      ]);
    }

    // Gastos de Caja Menor
    for (const exp of data.expenses) {
      rows.push([
        'GASTO',
        exp.id.substring(0, 8),
        exp.expenseDate || exp.createdAt,
        exp.description,
        exp.expenseType,
        exp.amount,
        exp.approvalStatus,
      ]);
    }

    // Cancelaciones con Devolución
    for (const ref of data.refunds) {
      rows.push([
        'DEVOLUCIÓN',
        ref.ticketNumber || ref.id,
        ref.createdAt,
        ref.cancellationReason || 'Cancelación',
        ref.paymentMethod,
        ref.totalAmount,
        'CANCELLED',
      ]);
    }

    exportToCsv(filename, headers, rows);
  }

  onClose(): void {
    this.closeModal.emit();
  }
}
