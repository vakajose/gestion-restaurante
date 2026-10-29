import { Component, computed, input } from '@angular/core';
import { IconComponent, SolarIconName } from '../icon/icon.component';

export type UiBadgeColor =
  | 'primary'
  | 'secondary'
  | 'success'
  | 'warning'
  | 'error'
  | 'info'
  | 'neutral';

export type UiBadgeVariant = 'soft' | 'solid' | 'outline';
export type UiBadgeSize = 'sm' | 'md' | 'lg';

/**
 * Insignia Semántica de Alto Contraste WCAG 2.2 AA (OBS-UI-06 / ADR-0014).
 * Utiliza tonos oscurecidos para el texto en modo claro y luminosos en modo oscuro
 * para garantizar legibilidad perfecta en cualquier superficie.
 */
@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [IconComponent],
  template: `
    <span [class]="computedClasses()">
      @if (dot()) {
        <span [class]="dotClasses()" aria-hidden="true"></span>
      }
      @if (icon()) {
        <app-icon [name]="icon()!" size="xs"></app-icon>
      }
      <ng-content></ng-content>
    </span>
  `,
})
export class BadgeComponent {
  readonly color = input<UiBadgeColor>('primary');
  readonly variant = input<UiBadgeVariant>('soft');
  readonly size = input<UiBadgeSize>('md');
  readonly dot = input<boolean>(false);
  readonly pill = input<boolean>(false);
  readonly icon = input<SolarIconName | undefined>(undefined);

  readonly computedClasses = computed<string>(() => {
    const radius = this.pill() || this.dot() ? 'rounded-full' : 'rounded-sm';
    const sizeClasses: Record<UiBadgeSize, string> = {
      sm: 'text-[11px] px-2 py-0.5 gap-1',
      md: 'text-xs px-2.5 py-1 gap-1.5',
      lg: 'text-sm px-3 py-1 gap-1.5',
    };

    const softPalette: Record<UiBadgeColor, string> = {
      primary:
        'bg-lightprimary text-primary-emphasis border border-primary/20 dark:bg-primary/20 dark:text-[#93b4ff] dark:border-primary/35',
      secondary:
        'bg-lightsecondary text-[#0369a1] border border-secondary/25 dark:bg-secondary/20 dark:text-sky-300 dark:border-secondary/35',
      success:
        'bg-[#dcfce7] text-[#15803d] border border-[#bbf7d0] dark:bg-emerald-500/20 dark:text-emerald-300 dark:border-emerald-500/35',
      warning:
        'bg-[#fef3c7] text-[#b45309] border border-[#fde68a] dark:bg-amber-500/20 dark:text-amber-300 dark:border-amber-500/35',
      error:
        'bg-[#ffe4e6] text-[#be123c] border border-[#fecdd3] dark:bg-rose-500/20 dark:text-rose-300 dark:border-rose-500/35',
      info:
        'bg-[#e0f2fe] text-[#0369a1] border border-[#bae6fd] dark:bg-sky-500/20 dark:text-sky-300 dark:border-sky-500/35',
      neutral:
        'bg-slate-100 text-slate-700 border border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
    };

    const solidPalette: Record<UiBadgeColor, string> = {
      primary: 'bg-primary text-white border border-transparent',
      secondary: 'bg-secondary text-white border border-transparent',
      success: 'bg-[#16a34a] text-white border border-transparent',
      warning: 'bg-[#d97706] text-white border border-transparent',
      error: 'bg-[#e11d48] text-white border border-transparent',
      info: 'bg-[#0284c7] text-white border border-transparent',
      neutral: 'bg-slate-700 text-white border border-transparent',
    };

    const outlinePalette: Record<UiBadgeColor, string> = {
      primary: 'bg-transparent border border-primary text-primary-emphasis dark:text-[#93b4ff]',
      secondary: 'bg-transparent border border-secondary text-[#0369a1] dark:text-sky-300',
      success: 'bg-transparent border border-[#16a34a] text-[#15803d] dark:text-emerald-300',
      warning: 'bg-transparent border border-[#d97706] text-[#b45309] dark:text-amber-300',
      error: 'bg-transparent border border-[#e11d48] text-[#be123c] dark:text-rose-300',
      info: 'bg-transparent border border-[#0284c7] text-[#0369a1] dark:text-sky-300',
      neutral: 'bg-transparent border border-slate-300 text-slate-700 dark:border-slate-600 dark:text-slate-300',
    };

    const palette =
      this.variant() === 'solid'
        ? solidPalette
        : this.variant() === 'outline'
          ? outlinePalette
          : softPalette;

    return `inline-flex items-center font-semibold leading-none whitespace-nowrap ${radius} ${sizeClasses[this.size()]} ${palette[this.color()]}`;
  });

  readonly dotClasses = computed<string>(() => {
    const dotColors: Record<UiBadgeColor, string> = {
      primary: 'bg-primary',
      secondary: 'bg-secondary',
      success: 'bg-[#16a34a] dark:bg-emerald-400',
      warning: 'bg-[#d97706] dark:bg-amber-400',
      error: 'bg-[#e11d48] dark:bg-rose-400',
      info: 'bg-[#0284c7] dark:bg-sky-400',
      neutral: 'bg-slate-500 dark:bg-slate-400',
    };
    return `w-1.5 h-1.5 rounded-full shrink-0 ${dotColors[this.color()]}`;
  });
}
