import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { ConfigurationService } from '../../../core/services/configuration.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import { AuthService } from '../../../core/services/auth.service';
import { BadgeComponent } from '../../../shared/components/ui/badge/badge.component';
import {
  ParameterCategory,
  ParameterScope,
  SystemParameterDto,
} from '../../../core/models/configuration.model';

@Component({
  selector: 'app-parameters',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BadgeComponent],
  templateUrl: './parameters.component.html',
})
export class ParametersComponent implements OnInit, OnDestroy {
  readonly configService = inject(ConfigurationService);
  readonly branchContext = inject(BranchContextService);
  readonly authService = inject(AuthService);

  // Estados Locales
  readonly searchInput = signal<string>('');
  readonly feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);
  readonly isSavingKey = signal<string | null>(null);

  // Modal para Parámetros Críticos (HIGH Criticality)
  readonly isCriticalModalOpen = signal<boolean>(false);
  readonly criticalParam = signal<SystemParameterDto | null>(null);
  readonly criticalNewValue = signal<string>('');
  readonly passwordConfirmation = signal<string>('');
  readonly auditReason = signal<string>('');
  readonly criticalError = signal<string | null>(null);
  readonly isConfirmingCritical = signal<boolean>(false);

  // Debounce para el buscador en tiempo real
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  // Valores de edición en línea temporales para inputs de texto/número
  readonly editValues = signal<Record<string, string>>({});

  readonly categories: { id: ParameterCategory | 'ALL'; label: string; icon: string }[] = [
    { id: 'ALL', label: 'Todos', icon: '📋' },
    { id: 'POS', label: 'Punto de Venta', icon: '💳' },
    { id: 'INVENTORY', label: 'Inventario', icon: '📦' },
    { id: 'FINANCE', label: 'Finanzas', icon: '💰' },
    { id: 'SECURITY', label: 'Seguridad', icon: '🔒' },
    { id: 'GENERAL', label: 'General', icon: '🌐' },
  ];

  ngOnInit(): void {
    // Suscripción al buscador con debounce de 250ms
    this.searchSub = this.searchSubject
      .pipe(debounceTime(250), distinctUntilChanged())
      .subscribe((query) => {
        this.configService.setSearchQuery(query);
      });

    this.refresh();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  refresh(): void {
    const scope = this.configService.selectedScope();
    const branchId = scope === 'BRANCH' ? this.branchContext.activeBranchId() : null;
    this.configService.loadParameters(scope, null, branchId).subscribe({
      next: (params) => {
        // Inicializar mapa de valores de edición
        const values: Record<string, string> = {};
        params.forEach((p) => {
          values[p.key] = p.effectiveValue;
        });
        this.editValues.set(values);
      },
      error: () => {
        this.showFeedback('error', 'Error al cargar los parámetros del sistema.');
      },
    });
  }

  onSearchChange(value: string): void {
    this.searchInput.set(value);
    this.searchSubject.next(value);
  }

  setScope(scope: ParameterScope): void {
    this.configService.setSelectedScope(scope);
    this.refresh();
  }

  setCategory(category: ParameterCategory | 'ALL'): void {
    this.configService.setSelectedCategory(category);
  }

  // Actualización de parámetro booleano (Switch Toggle)
  toggleBoolean(param: SystemParameterDto): void {
    const nextValue = param.effectiveValue.toLowerCase() === 'true' ? 'false' : 'true';
    if (param.criticality === 'HIGH') {
      this.openCriticalModal(param, nextValue);
    } else {
      this.applyUpdate(param.key, nextValue);
    }
  }

  // Actualización de enum
  onEnumChange(param: SystemParameterDto, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const nextValue = select.value;
    if (param.criticality === 'HIGH') {
      // Revertir temporalmente el select visual hasta confirmación
      select.value = param.effectiveValue;
      this.openCriticalModal(param, nextValue);
    } else {
      this.applyUpdate(param.key, nextValue);
    }
  }

  // Actualización de input de texto o número
  saveInlineValue(param: SystemParameterDto): void {
    const value = this.editValues()[param.key];
    if (value === undefined || value.trim() === param.effectiveValue) {
      return;
    }
    if (param.criticality === 'HIGH') {
      this.openCriticalModal(param, value.trim());
    } else {
      this.applyUpdate(param.key, value.trim());
    }
  }

  updateEditValue(key: string, value: string): void {
    this.editValues.update((current) => ({
      ...current,
      [key]: value,
    }));
  }

  // Restablecer al valor corporativo por defecto
  resetToDefault(param: SystemParameterDto): void {
    const scope = this.configService.selectedScope();
    const branchId = scope === 'BRANCH' ? this.branchContext.activeBranchId() : null;

    this.isSavingKey.set(param.key);
    this.configService.resetParameter(param.key, scope, branchId).subscribe({
      next: () => {
        this.isSavingKey.set(null);
        this.showFeedback('success', `Parámetro '${param.name}' restablecido al valor corporativo.`);
        this.refresh();
      },
      error: () => {
        this.isSavingKey.set(null);
        this.showFeedback('error', `Error al restablecer el parámetro '${param.name}'.`);
      },
    });
  }

  // Gestión de Modal de Criticidad Alta (UC-311.3)
  openCriticalModal(param: SystemParameterDto, nextValue: string): void {
    this.criticalParam.set(param);
    this.criticalNewValue.set(nextValue);
    this.passwordConfirmation.set('');
    this.auditReason.set('');
    this.criticalError.set(null);
    this.isCriticalModalOpen.set(true);
  }

  closeCriticalModal(): void {
    this.isCriticalModalOpen.set(false);
    this.criticalParam.set(null);
    this.criticalNewValue.set('');
    this.passwordConfirmation.set('');
    this.auditReason.set('');
    this.criticalError.set(null);
  }

  confirmCriticalChange(): void {
    const param = this.criticalParam();
    const newValue = this.criticalNewValue();
    const password = this.passwordConfirmation().trim();
    const reason = this.auditReason().trim();

    if (!param) {
      return;
    }

    if (!password) {
      this.criticalError.set('Debe ingresar su contraseña de confirmación.');
      return;
    }

    if (reason.length < 10) {
      this.criticalError.set('Debe ingresar un motivo justificado de al menos 10 caracteres.');
      return;
    }

    this.isConfirmingCritical.set(true);
    this.criticalError.set(null);

    const scope = this.configService.selectedScope();
    const branchId = scope === 'BRANCH' ? this.branchContext.activeBranchId() : null;

    this.configService
      .updateParameter(param.key, {
        scope,
        branchId,
        value: newValue,
        passwordConfirmation: password,
        auditReason: reason,
      })
      .subscribe({
        next: () => {
          this.isConfirmingCritical.set(false);
          this.closeCriticalModal();
          this.showFeedback('success', `Parámetro crítico '${param.name}' actualizado exitosamente.`);
          this.refresh();
        },
        error: (err) => {
          this.isConfirmingCritical.set(false);
          const detail = err?.error?.detail || err?.message || 'Error de reautenticación o validación.';
          this.criticalError.set(detail);
        },
      });
  }

  private applyUpdate(key: string, value: string): void {
    const scope = this.configService.selectedScope();
    const branchId = scope === 'BRANCH' ? this.branchContext.activeBranchId() : null;

    this.isSavingKey.set(key);
    this.configService
      .updateParameter(key, {
        scope,
        branchId,
        value,
      })
      .subscribe({
        next: (updated) => {
          this.isSavingKey.set(null);
          this.showFeedback('success', `Parámetro '${updated.name}' guardado correctamente.`);
        },
        error: (err) => {
          this.isSavingKey.set(null);
          const detail = err?.error?.detail || 'Error al actualizar el parámetro.';
          this.showFeedback('error', detail);
        },
      });
  }

  private showFeedback(type: 'success' | 'error', message: string): void {
    this.feedback.set({ type, message });
    setTimeout(() => {
      this.feedback.set(null);
    }, 4500);
  }

  clearFeedback(): void {
    this.feedback.set(null);
  }
}
