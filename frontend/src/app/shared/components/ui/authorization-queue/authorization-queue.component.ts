import { Component, computed, input, model, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShiftAuthorizationDto } from '../../../../core/models/finance.models';
import { BadgeComponent, UiBadgeColor } from '../badge/badge.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-authorization-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, BadgeComponent, ButtonComponent, IconComponent],
  template: `
    <div class="space-y-4" data-testid="authorization-queue-container">
      <!-- Filtros de Cabecera -->
      <div class="flex flex-wrap items-center justify-between gap-3 p-3 bg-surface border border-border rounded-xl">
        <div class="flex flex-wrap items-center gap-3">
          <div>
            <label for="filterTypeSelect" class="block text-xs font-medium text-text-muted mb-1">Tipo de Solicitud</label>
            <select
              id="filterTypeSelect"
              [(ngModel)]="filterType"
              data-testid="filter-type-select"
              class="text-xs bg-canvas border border-border rounded-lg px-2.5 py-1.5 text-text focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="ALL">Todos los Tipos</option>
              <option value="ORDER_CANCELLATION">Cancelación de Pedido</option>
              <option value="EXPENSE">Gasto Excedente</option>
            </select>
          </div>

          <div>
            <label for="filterStatusSelect" class="block text-xs font-medium text-text-muted mb-1">Estado</label>
            <select
              id="filterStatusSelect"
              [(ngModel)]="filterStatus"
              data-testid="filter-status-select"
              class="text-xs bg-canvas border border-border rounded-lg px-2.5 py-1.5 text-text focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="ALL">Todos los Estados</option>
              <option value="PENDING">Pendientes</option>
              <option value="APPROVED">Aprobadas</option>
              <option value="REJECTED">Rechazadas</option>
              <option value="EXPIRED">Expiradas</option>
            </select>
          </div>
        </div>

        <div class="text-xs text-text-muted">
          Mostrando {{ filteredAuthorizations().length }} de {{ authorizations().length }} solicitudes
        </div>
      </div>

      <!-- Listado de Solicitudes -->
      @if (filteredAuthorizations().length === 0) {
        <div class="p-8 text-center bg-surface border border-dashed border-border rounded-xl" data-testid="empty-queue-state">
          <app-icon name="shield-user" size="lg" class="mx-auto text-text-muted/60 mb-2"></app-icon>
          <p class="text-sm font-medium text-text">No hay solicitudes de autorización</p>
          <p class="text-xs text-text-muted mt-1">No se encontraron registros con los filtros seleccionados.</p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (auth of filteredAuthorizations(); track auth.id) {
            <div
              class="p-4 bg-surface border border-border rounded-xl shadow-xs hover:border-primary/40 transition-colors"
              data-testid="authorization-item"
              [attr.data-auth-id]="auth.id"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <!-- Info Principal -->
                <div class="space-y-1.5 min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <app-badge [color]="getTypeBadgeColor(auth.requestType)" size="sm">
                      {{ getTypeName(auth.requestType) }}
                    </app-badge>

                    <app-badge [color]="getStatusBadgeColor(auth.status)" [dot]="true" size="sm">
                      {{ getStatusName(auth.status) }}
                    </app-badge>

                    @if (auth.amount !== null && auth.amount !== undefined) {
                      <span class="text-sm font-semibold text-text" data-testid="auth-amount">
                        Bs. {{ auth.amount.toFixed(2) }}
                      </span>
                    }
                  </div>

                  <p class="text-xs text-text-muted">
                    Solicitado por: <span class="font-medium text-text">{{ auth.requestedByName || auth.requestedBy }}</span>
                    · {{ formatDate(auth.createdAt) }}
                  </p>

                  @if (auth.justification) {
                    <div class="text-xs text-text bg-canvas/60 border border-border/50 rounded-lg p-2.5 mt-1">
                      <span class="font-medium text-text-muted block text-[11px] uppercase tracking-wider mb-0.5">Motivo / Justificación:</span>
                      {{ auth.justification }}
                    </div>
                  }

                  <!-- Resolución si ya fue decidida -->
                  @if (auth.status !== 'PENDING') {
                    <div class="text-xs pt-1 border-t border-border/60 text-text-muted flex flex-wrap gap-2 items-center">
                      <span>Decidido por: <strong class="text-text">{{ auth.decidedByName || auth.decidedBy || 'Sistema' }}</strong></span>
                      @if (auth.decisionChannel) {
                        <span>({{ auth.decisionChannel === 'IN_SITU_PIN' ? 'PIN Presencial' : 'Canal Remoto' }})</span>
                      }
                      @if (auth.decisionNotes) {
                        <span class="italic">"{{ auth.decisionNotes }}"</span>
                      }
                    </div>
                  }
                </div>

                <!-- Acciones según Modo de Vista -->
                <div class="shrink-0 flex items-center gap-2">
                  <!-- Acciones SUPERVISOR -->
                  @if (viewMode() === 'SUPERVISOR' && auth.status === 'PENDING') {
                    @if (rejectingId() === auth.id) {
                      <div class="flex flex-col gap-2 p-2 bg-canvas border border-border rounded-lg min-w-[220px]">
                        <input
                          type="text"
                          [(ngModel)]="rejectReasonText"
                          placeholder="Motivo del rechazo..."
                          class="text-xs bg-surface border border-border rounded px-2 py-1 text-text focus:outline-none focus:ring-1 focus:ring-error"
                          data-testid="input-reject-reason"
                        />
                        <div class="flex items-center gap-2 justify-end">
                          <app-button
                            variant="ghost"
                            size="sm"
                            (click)="cancelRejectPrompt()"
                          >
                            Cancelar
                          </app-button>
                          <app-button
                            variant="danger"
                            size="sm"
                            data-testid="btn-confirm-reject"
                            [disabled]="!rejectReasonText.trim()"
                            (click)="confirmReject(auth.id)"
                          >
                            Rechazar
                          </app-button>
                        </div>
                      </div>
                    } @else {
                      <div class="flex items-center gap-2">
                        <app-button
                          variant="neutral"
                          size="sm"
                          data-testid="btn-reject-auth"
                          (click)="startRejectPrompt(auth.id)"
                        >
                          <app-icon name="close-circle" size="xs"></app-icon>
                          Rechazar
                        </app-button>

                        <app-button
                          variant="primary"
                          size="sm"
                          data-testid="btn-approve-auth"
                          (click)="confirmApprove(auth.id)"
                        >
                          <app-icon name="check-circle" size="xs"></app-icon>
                          Aprobar
                        </app-button>
                      </div>
                    }
                  }

                  <!-- Acciones CASHIER -->
                  @if (viewMode() === 'CASHIER' && auth.status === 'PENDING') {
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-warning animate-pulse flex items-center gap-1 font-medium">
                        <app-icon name="lock" size="xs"></app-icon>
                        En espera
                      </span>
                      <app-button
                        variant="neutral"
                        size="sm"
                        data-testid="btn-request-pin"
                        (click)="requestPin.emit(auth.id)"
                      >
                        <app-icon name="shield-user" size="xs"></app-icon>
                        Ingresar PIN
                      </app-button>
                    </div>
                  }
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class AuthorizationQueueComponent {
  // Inputs
  readonly authorizations = input.required<ShiftAuthorizationDto[]>();
  readonly viewMode = input<'CASHIER' | 'SUPERVISOR'>('CASHIER');
  readonly filterType = model<string>('ALL');
  readonly filterStatus = model<string>('ALL');

  // Outputs
  readonly approve = output<{ id: string; notes?: string }>();
  readonly reject = output<{ id: string; reason: string }>();
  readonly requestPin = output<string>();

  // Estado interno para rechazo
  readonly rejectingId = signal<string | null>(null);
  rejectReasonText = '';

  // Filtro reactivo computado
  readonly filteredAuthorizations = computed(() => {
    let list = this.authorizations();
    const type = this.filterType();
    const status = this.filterStatus();

    if (type !== 'ALL') {
      list = list.filter((a) => a.requestType === type);
    }
    if (status !== 'ALL') {
      list = list.filter((a) => a.status === status);
    }
    return list;
  });

  getTypeName(type: string): string {
    switch (type) {
      case 'ORDER_CANCELLATION':
        return 'Cancelación de Pedido';
      case 'EXPENSE':
        return 'Gasto Excedente';
      default:
        return type;
    }
  }

  getTypeBadgeColor(type: string): UiBadgeColor {
    switch (type) {
      case 'ORDER_CANCELLATION':
        return 'warning';
      case 'EXPENSE':
        return 'info';
      default:
        return 'neutral';
    }
  }

  getStatusName(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Pendiente';
      case 'APPROVED':
        return 'Aprobada';
      case 'REJECTED':
        return 'Rechazada';
      case 'EXPIRED':
        return 'Expirada';
      default:
        return status;
    }
  }

  getStatusBadgeColor(status: string): UiBadgeColor {
    switch (status) {
      case 'PENDING':
        return 'warning';
      case 'APPROVED':
        return 'success';
      case 'REJECTED':
        return 'error';
      case 'EXPIRED':
        return 'neutral';
      default:
        return 'neutral';
    }
  }

  formatDate(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('es-BO', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  }

  confirmApprove(id: string): void {
    this.approve.emit({ id, notes: 'Aprobado desde consola de supervisión' });
  }

  startRejectPrompt(id: string): void {
    this.rejectingId.set(id);
    this.rejectReasonText = '';
  }

  cancelRejectPrompt(): void {
    this.rejectingId.set(null);
    this.rejectReasonText = '';
  }

  confirmReject(id: string): void {
    const reason = this.rejectReasonText.trim();
    if (!reason) return;
    this.reject.emit({ id, reason });
    this.rejectingId.set(null);
    this.rejectReasonText = '';
  }
}
