import { Component, computed, input, output } from '@angular/core';
import { IconComponent, SolarIconName } from '../icon/icon.component';

export type UiButtonVariant =
  | 'primary'
  | 'neutral'
  | 'secondary'
  | 'soft-primary'
  | 'outline-primary'
  | 'danger'
  | 'soft-danger'
  | 'success'
  | 'ghost';

export type UiButtonSize = 'xs' | 'sm' | 'md' | 'lg' | 'icon';

/**
 * Componente de Botón Estandarizado de TailwindAdmin v3.0 (OBS-UI-05 / ADR-0014).
 * Garantiza distinción semántica estricta entre acciones afirmativas ('primary'),
 * acciones de cancelación/retorno ('neutral') y destructivas ('danger').
 */
@Component({
  selector: 'app-button',
  standalone: true,
  imports: [IconComponent],
  template: `
    <button
      [attr.type]="type()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading()"
      [attr.aria-label]="ariaLabel() || null"
      [class]="computedClasses()"
      (click)="handleClick($event)"
    >
      @if (loading()) {
        <svg class="animate-spin h-4 w-4 shrink-0" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      } @else if (iconLeft()) {
        <app-icon [name]="iconLeft()!" [size]="size() === 'xs' ? 'xs' : 'sm'"></app-icon>
      }

      <ng-content></ng-content>

      @if (!loading() && iconRight()) {
        <app-icon [name]="iconRight()!" [size]="size() === 'xs' ? 'xs' : 'sm'"></app-icon>
      }
    </button>
  `,
})
export class ButtonComponent {
  readonly variant = input<UiButtonVariant>('primary');
  readonly size = input<UiButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);
  readonly iconLeft = input<SolarIconName | undefined>(undefined);
  readonly iconRight = input<SolarIconName | undefined>(undefined);
  readonly ariaLabel = input<string>('');
  readonly extraClass = input<string>('');

  readonly btnClick = output<MouseEvent>();

  readonly computedClasses = computed<string>(() => {
    const base =
      'inline-flex items-center justify-center font-medium rounded-md transition-all duration-150 cursor-pointer select-none focus:outline-none focus:ring-2 focus:ring-primary/25 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap';

    const sizeMap: Record<UiButtonSize, string> = {
      xs: 'text-xs px-2.5 py-1.5 gap-1.5 min-h-[30px]',
      sm: 'text-xs px-3 py-2 gap-1.5 min-h-[36px]',
      md: 'text-sm px-4 py-2.5 gap-2 min-h-[40px]',
      lg: 'text-base px-5 py-3 gap-2.5 min-h-[44px]',
      icon: 'p-2 min-h-[38px] min-w-[38px]',
    };

    const variantMap: Record<UiButtonVariant, string> = {
      primary:
        'bg-primary hover:bg-primary-hover active:bg-primary-emphasis text-white border border-transparent shadow-xs',
      neutral:
        'bg-surface hover:bg-canvas text-bodytext hover:text-link dark:text-darklink dark:hover:text-white border border-bordergray dark:border-darkborder shadow-2xs',
      secondary:
        'bg-secondary hover:bg-secondary-hover text-white border border-transparent shadow-xs',
      'soft-primary':
        'bg-lightprimary text-primary-emphasis hover:bg-primary hover:text-white border border-primary/15',
      'outline-primary':
        'bg-transparent border border-primary text-primary hover:bg-primary hover:text-white',
      danger:
        'bg-error hover:bg-error-hover text-white border border-transparent shadow-xs',
      'soft-danger':
        'bg-lighterror text-error-emphasis hover:bg-error hover:text-white border border-error/20',
      success:
        'bg-success hover:bg-success-hover text-white border border-transparent shadow-xs',
      ghost:
        'bg-transparent hover:bg-canvas text-bodytext hover:text-link dark:text-darklink dark:hover:text-white border border-transparent',
    };

    const widthClass = this.fullWidth() ? 'w-full' : '';
    return `${base} ${sizeMap[this.size()]} ${variantMap[this.variant()]} ${widthClass} ${this.extraClass()}`.trim();
  });

  handleClick(event: MouseEvent): void {
    if (this.disabled() || this.loading()) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.btnClick.emit(event);
  }
}
