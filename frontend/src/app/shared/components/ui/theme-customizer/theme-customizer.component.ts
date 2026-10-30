import { Component, HostListener, inject, signal } from '@angular/core';
import {
  BrandPalette,
  SidebarCollapseMode,
  SurfacePreset,
  ThemeService,
} from '../../../../core/services/theme.service';
import { IconComponent } from '../icon/icon.component';

interface SurfaceOption {
  id: SurfacePreset;
  label: string;
  description: string;
}

interface BrandOption {
  id: BrandPalette;
  label: string;
  swatchHex: string;
}

/**
 * Selector de Tema y Apariencia en Caliente (Zero-Reload / ADR-0014).
 * Permite alternar en tiempo real entre Modo Claro/Oscuro, Contraste de Superficie
 * (Opción 1 Pizarra Nítido, Opción 2 Cálido Tinteado, Opción 3 Sidebar Oscuro),
 * Color Primario de Marca y Comportamiento del Menú Lateral en Desktop.
 */
@Component({
  selector: 'app-theme-customizer',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="relative">
      <button
        type="button"
        data-testid="theme-customizer-btn"
        (click)="toggleOpen()"
        class="p-2 rounded-md hover:bg-canvas text-bodytext dark:text-darklink hover:text-primary transition-colors cursor-pointer flex items-center justify-center"
        title="Personalizar apariencia y contraste del tema"
        aria-label="Personalizar apariencia y contraste del tema"
        [attr.aria-expanded]="isOpen()"
      >
        <app-icon name="palette" size="md"></app-icon>
      </button>

      @if (isOpen()) {
        <div
          class="fixed inset-0 z-40"
          (click)="close()"
          aria-hidden="true"
        ></div>

        <div
          data-testid="theme-customizer-panel"
          class="absolute right-0 mt-2 w-80 rounded-lg shadow-xl bg-surface border border-border p-4 z-50 text-xs space-y-4"
        >
          <div class="flex items-center justify-between border-b border-border pb-2.5">
            <div>
              <h3 class="text-sm font-bold text-link dark:text-white">Apariencia y Tema</h3>
              <p class="text-[11px] text-bodytext dark:text-darklink">Ajuste instantáneo en caliente</p>
            </div>
            <button
              type="button"
              data-testid="theme-reset-btn"
              (click)="themeService.resetToDefaults()"
              class="text-[11px] text-primary hover:underline font-medium cursor-pointer"
            >
              Restaurar
            </button>
          </div>

          <!-- 1. Modo de Luminosidad -->
          <div class="space-y-1.5">
            <span class="font-semibold uppercase tracking-wider text-bodytext dark:text-darklink text-[10px]">
              1. Modo de Brillo
            </span>
            <div class="grid grid-cols-2 gap-2">
              <button
                type="button"
                data-testid="theme-mode-light"
                (click)="themeService.setTheme(false)"
                [class.border-primary]="!themeService.isDarkMode()"
                [class.bg-lightprimary]="!themeService.isDarkMode()"
                [class.text-primary-emphasis]="!themeService.isDarkMode()"
                class="flex items-center justify-center gap-2 py-2 px-3 rounded-md border border-border hover:border-primary transition-all font-semibold cursor-pointer"
              >
                <app-icon name="sun" size="sm"></app-icon>
                <span>Claro</span>
              </button>
              <button
                type="button"
                data-testid="theme-mode-dark"
                (click)="themeService.setTheme(true)"
                [class.border-primary]="themeService.isDarkMode()"
                [class.bg-lightprimary]="themeService.isDarkMode()"
                [class.text-primary]="themeService.isDarkMode()"
                class="flex items-center justify-center gap-2 py-2 px-3 rounded-md border border-border hover:border-primary transition-all font-semibold cursor-pointer"
              >
                <app-icon name="moon" size="sm"></app-icon>
                <span>Oscuro</span>
              </button>
            </div>
          </div>

          <!-- 2. Contraste de Superficie (Modo Claro) -->
          <div class="space-y-1.5">
            <span class="font-semibold uppercase tracking-wider text-bodytext dark:text-darklink text-[10px]">
              2. Jerarquía de Superficies
            </span>
            <div class="space-y-1.5">
              @for (opt of surfaceOptions; track opt.id) {
                <button
                  type="button"
                  [attr.data-testid]="'surface-preset-' + opt.id"
                  (click)="themeService.setSurfacePreset(opt.id)"
                  [class.border-primary]="themeService.surfacePreset() === opt.id"
                  [class.bg-lightprimary]="themeService.surfacePreset() === opt.id"
                  class="w-full text-left p-2.5 rounded-md border border-border hover:border-primary transition-all flex items-center justify-between gap-2 cursor-pointer"
                >
                  <div>
                    <p class="font-semibold text-link dark:text-white">{{ opt.label }}</p>
                    <p class="text-[11px] text-bodytext dark:text-darklink">{{ opt.description }}</p>
                  </div>
                  @if (themeService.surfacePreset() === opt.id) {
                    <span class="text-primary font-bold">✓</span>
                  }
                </button>
              }
            </div>
          </div>

          <!-- 3. Color de Marca -->
          <div class="space-y-1.5">
            <span class="font-semibold uppercase tracking-wider text-bodytext dark:text-darklink text-[10px]">
              3. Color de Marca
            </span>
            <div class="flex items-center justify-between gap-2 pt-1">
              @for (brand of brandOptions; track brand.id) {
                <button
                  type="button"
                  [attr.data-testid]="'brand-palette-' + brand.id"
                  (click)="themeService.setBrandPalette(brand.id)"
                  [title]="brand.label"
                  [class.ring-2]="themeService.brandPalette() === brand.id"
                  [class.ring-offset-2]="themeService.brandPalette() === brand.id"
                  class="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold shadow-xs transition-transform hover:scale-105 cursor-pointer"
                  [style.background-color]="brand.swatchHex"
                >
                  @if (themeService.brandPalette() === brand.id) {
                    <span>✓</span>
                  }
                </button>
              }
            </div>
          </div>

          <!-- 4. Modo de Colapso del Menú en Desktop -->
          <div class="space-y-1.5 pt-1 border-t border-border">
            <span class="font-semibold uppercase tracking-wider text-bodytext dark:text-darklink text-[10px]">
              4. Colapso de Menú en Escritorio
            </span>
            <div class="grid grid-cols-2 gap-2">
              <button
                type="button"
                data-testid="sidebar-mode-mini-rail"
                (click)="setCollapseMode('mini-rail')"
                [class.border-primary]="themeService.sidebarCollapseMode() === 'mini-rail'"
                [class.bg-lightprimary]="themeService.sidebarCollapseMode() === 'mini-rail'"
                class="py-2 px-2.5 rounded-md border border-border hover:border-primary transition-all font-medium text-center cursor-pointer"
              >
                Mini-Sidebar (76px)
              </button>
              <button
                type="button"
                data-testid="sidebar-mode-full-hide"
                (click)="setCollapseMode('full-hide')"
                [class.border-primary]="themeService.sidebarCollapseMode() === 'full-hide'"
                [class.bg-lightprimary]="themeService.sidebarCollapseMode() === 'full-hide'"
                class="py-2 px-2.5 rounded-md border border-border hover:border-primary transition-all font-medium text-center cursor-pointer"
              >
                Ocultar Total (0px)
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
})
export class ThemeCustomizerComponent {
  readonly themeService = inject(ThemeService);
  readonly isOpen = signal<boolean>(false);

  readonly surfaceOptions: SurfaceOption[] = [
    {
      id: 'slate-clean',
      label: 'Pizarra Nítido (Recomendado)',
      description: 'Fondo neutro con tarjetas y navegación blancas de alta nitidez',
    },
    {
      id: 'warm-tinted',
      label: 'Sidebar Tinteado Suave',
      description: 'Contraste sutil con menú lateral suavemente tonalizado',
    },
    {
      id: 'dark-nav',
      label: 'Sidebar Oscuro ERP',
      description: 'Menú lateral oscuro con área de trabajo clara',
    },
  ];

  readonly brandOptions: BrandOption[] = [
    { id: 'blue', label: 'Azul Clásico', swatchHex: '#5d87ff' },
    { id: 'aqua', label: 'Océano / Aqua', swatchHex: '#0074ba' },
    { id: 'purple', label: 'Púrpura Real', swatchHex: '#763ebd' },
    { id: 'emerald', label: 'Esmeralda Fresco', swatchHex: '#0f766e' },
    { id: 'orange', label: 'Naranja Gastronómico', swatchHex: '#ea580c' },
  ];

  toggleOpen(): void {
    this.isOpen.update((v) => !v);
  }

  close(): void {
    this.isOpen.set(false);
  }

  setCollapseMode(mode: SidebarCollapseMode): void {
    this.themeService.setSidebarCollapseMode(mode);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }
}
