import {
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ParameterHelpDto } from '../../../../core/models/configuration.model';

@Component({
  selector: 'app-context-help',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (help(); as h) {
      <div class="relative inline-block align-middle">
        <button
          type="button"
          (click)="toggle($event)"
          class="inline-flex items-center justify-center w-5 h-5 rounded-full text-[11px] font-bold border transition-colors cursor-pointer select-none"
          [class.bg-lightprimary]="!isOpen()"
          [class.text-primary]="!isOpen()"
          [class.border-primary/30]="!isOpen()"
          [class.bg-primary]="isOpen()"
          [class.text-white]="isOpen()"
          [class.border-primary]="isOpen()"
          [attr.aria-expanded]="isOpen()"
          aria-haspopup="dialog"
          title="Ayuda contextual y casos de uso"
          data-testid="context-help-btn"
        >
          ?
        </button>

        @if (isOpen()) {
          <div
            class="absolute z-50 left-0 sm:left-auto sm:right-0 mt-2 w-80 sm:w-96 p-4 rounded-lg bg-white dark:bg-darkcard border border-border dark:border-darkborder shadow-xl space-y-3 text-xs text-bodytext dark:text-darklink animate-in fade-in duration-150"
            role="dialog"
            aria-modal="false"
            data-testid="context-help-popover"
            (click)="$event.stopPropagation()"
            (keydown)="$event.stopPropagation()"
          >
            <!-- Header -->
            <div class="flex items-center justify-between pb-2 border-b border-border dark:border-darkborder">
              <span class="font-bold text-link dark:text-white flex items-center gap-1.5 text-xs">
                <span>💡</span>
                <span>Ayuda Contextual</span>
                @if (paramName()) {
                  <span class="text-bodytext/70 dark:text-darklink/70 font-normal">({{ paramName() }})</span>
                }
              </span>
              <button
                type="button"
                (click)="close()"
                class="text-bodytext dark:text-darklink hover:text-link dark:hover:text-white p-1 leading-none text-xs"
                title="Cerrar ayuda"
                data-testid="context-help-close-btn"
              >
                ✕
              </button>
            </div>

            <!-- Impacto Operativo -->
            @if (h.impact) {
              <div class="space-y-1">
                <p class="font-semibold text-link dark:text-white flex items-center gap-1 text-[11px] uppercase tracking-wider">
                  <span>⚙️</span>
                  <span>Impacto Operativo</span>
                </p>
                <p class="leading-relaxed text-bodytext dark:text-darklink" data-testid="context-help-impact">
                  {{ h.impact }}
                </p>
              </div>
            }

            <!-- Ejemplo en Restaurante -->
            @if (h.example) {
              <div class="space-y-1 p-2.5 rounded-md bg-lightprimary/40 dark:bg-dark border border-primary/20">
                <p class="font-semibold text-primary dark:text-lightprimary flex items-center gap-1 text-[11px] uppercase tracking-wider">
                  <span>🍽️</span>
                  <span>Ejemplo en Restaurante</span>
                </p>
                <p class="leading-relaxed italic text-link dark:text-white text-xs" data-testid="context-help-example">
                  "{{ h.example }}"
                </p>
              </div>
            }
          </div>
        }
      </div>
    }
  `,
})
export class ContextHelpComponent {
  private readonly elementRef = inject(ElementRef);

  readonly help = input<ParameterHelpDto | null | undefined>();
  readonly paramName = input<string>('');

  readonly isOpen = signal<boolean>(false);

  toggle(event: Event): void {
    event.stopPropagation();
    this.isOpen.update((v) => !v);
  }

  close(): void {
    this.isOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.isOpen() && !this.elementRef.nativeElement.contains(event.target)) {
      this.close();
    }
  }

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }
}
