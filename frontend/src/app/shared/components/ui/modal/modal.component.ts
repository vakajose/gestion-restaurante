import { Component, computed, ElementRef, HostListener, inject, input, output } from '@angular/core';
import { IconComponent } from '../icon/icon.component';
import { ButtonComponent, UiButtonVariant } from '../button/button.component';

export type UiModalSize = 'sm' | 'md' | 'lg' | 'xl' | '2xl';

/**
 * Diálogo Modal Accesible TailwindAdmin v3.0 (OBS-UI-05, OBS-UI-08, OBS-UI-09 / ADR-0014).
 * - Se eleva en z-[100] (debajo del portal global de Toasts z-[9999]).
 * - Incluye banner sticky de resumen de errores con auto-scroll al primer campo inválido.
 * - Estandariza el footer con Cancelar ('neutral') a la izquierda y Confirmar ('primary'/'danger') a la derecha.
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [IconComponent, ButtonComponent],
  template: `
    @if (isOpen()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6 overflow-y-auto"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
      >
        <!-- Backdrop -->
        <div
          data-testid="ui-modal-backdrop"
          class="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
          (click)="onBackdropClick()"
          aria-hidden="true"
        ></div>

        <!-- Panel Modal -->
        <div
          data-testid="ui-modal-panel"
          [class]="panelClasses()"
        >
          <!-- Header Fijo -->
          <div class="px-6 py-4 border-b border-border flex items-center justify-between gap-4 shrink-0">
            <div class="min-w-0">
              <h2 class="text-lg font-bold text-link dark:text-white truncate">
                {{ title() }}
              </h2>
              @if (subtitle()) {
                <p class="text-xs text-bodytext dark:text-darklink mt-0.5">
                  {{ subtitle() }}
                </p>
              }
            </div>
            <button
              type="button"
              data-testid="ui-modal-close-btn"
              (click)="emitClose()"
              class="p-1.5 rounded-md text-bodytext hover:text-link dark:text-darklink dark:hover:text-white hover:bg-canvas transition-colors cursor-pointer"
              aria-label="Cerrar ventana modal"
            >
              <app-icon name="close" size="md"></app-icon>
            </button>
          </div>

          <!-- Banner Sticky de Resumen de Errores (OBS-UI-09) -->
          @if (formErrorSummary()) {
            <div
              data-testid="ui-modal-error-banner"
              class="px-6 py-3 bg-[#ffe4e6] dark:bg-rose-950/60 border-b border-[#fecdd3] dark:border-rose-800/60 text-[#be123c] dark:text-rose-300 text-xs font-medium flex items-center justify-between gap-3 shrink-0"
            >
              <div class="flex items-center gap-2">
                <app-icon name="danger-triangle" size="sm"></app-icon>
                <span>{{ formErrorSummary() }}</span>
              </div>
              <button
                type="button"
                (click)="scrollToFirstError()"
                class="underline font-semibold hover:opacity-80 shrink-0 cursor-pointer"
              >
                Ir al campo
              </button>
            </div>
          }

          <!-- Cuerpo Scrolleable -->
          <div class="p-6 overflow-y-auto max-h-[calc(85vh-145px)] space-y-4">
            <ng-content></ng-content>
          </div>

          <!-- Footer Estandarizado -->
          @if (showFooter()) {
            <div class="px-6 py-4 border-t border-border bg-canvas/50 flex items-center justify-end gap-3 shrink-0 rounded-b-lg">
              <ng-content select="[modalFooter]"></ng-content>

              @if (!hasCustomFooter()) {
                <app-button
                  variant="neutral"
                  data-testid="ui-modal-cancel-btn"
                  [disabled]="confirmLoading()"
                  (btnClick)="emitClose()"
                >
                  {{ cancelLabel() }}
                </app-button>

                <app-button
                  [variant]="confirmVariant()"
                  data-testid="ui-modal-confirm-btn"
                  [loading]="confirmLoading()"
                  [disabled]="confirmDisabled()"
                  (btnClick)="confirmModal.emit()"
                >
                  {{ confirmLabel() }}
                </app-button>
              }
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class ModalComponent {
  private readonly hostEl = inject(ElementRef<HTMLElement>);

  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly size = input<UiModalSize>('md');
  readonly closeOnBackdrop = input<boolean>(true);
  readonly showFooter = input<boolean>(true);
  readonly hasCustomFooter = input<boolean>(false);
  readonly cancelLabel = input<string>('Cancelar');
  readonly confirmLabel = input<string>('Guardar');
  readonly confirmVariant = input<UiButtonVariant>('primary');
  readonly confirmLoading = input<boolean>(false);
  readonly confirmDisabled = input<boolean>(false);
  readonly formErrorSummary = input<string | null>(null);

  readonly closeModal = output<void>();
  readonly confirmModal = output<void>();

  readonly panelClasses = computed<string>(() => {
    const sizeMap: Record<UiModalSize, string> = {
      sm: 'max-w-md',
      md: 'max-w-lg',
      lg: 'max-w-2xl',
      xl: 'max-w-4xl',
      '2xl': 'max-w-6xl',
    };
    return `relative z-10 w-full ${sizeMap[this.size()]} bg-surface border border-border rounded-lg shadow-xl flex flex-col overflow-hidden transition-all`;
  });

  emitClose(): void {
    this.closeModal.emit();
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen() && !this.confirmLoading()) {
      this.emitClose();
    }
  }

  onBackdropClick(): void {
    if (this.closeOnBackdrop() && !this.confirmLoading()) {
      this.emitClose();
    }
  }

  scrollToFirstError(): void {
    const root = this.hostEl.nativeElement as HTMLElement;
    const firstErrorEl = root.querySelector('.has-error, .is-invalid, [aria-invalid="true"]') as HTMLElement | null;
    if (firstErrorEl && typeof firstErrorEl.scrollIntoView === 'function') {
      firstErrorEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      const focusable = firstErrorEl.querySelector('input, select, textarea') as HTMLElement | null;
      focusable?.focus();
    }
  }
}
