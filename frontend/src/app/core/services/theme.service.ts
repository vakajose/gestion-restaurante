import { Injectable, signal, effect, inject, PLATFORM_ID, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export const THEME_STORAGE_KEY = 'app_theme';
export const UI_PREFERENCES_STORAGE_KEY = 'app_ui_preferences';

export type SurfacePreset = 'slate-clean' | 'warm-tinted' | 'dark-nav';
export type BrandPalette = 'blue' | 'aqua' | 'purple' | 'emerald' | 'orange';
export type SidebarCollapseMode = 'mini-rail' | 'full-hide';

export interface UiThemePreferences {
  themeMode: 'light' | 'dark';
  surfacePreset: SurfacePreset;
  brandPalette: BrandPalette;
  sidebarCollapseMode: SidebarCollapseMode;
  isDesktopSidebarCollapsed: boolean;
}

const DEFAULT_PREFERENCES: UiThemePreferences = {
  themeMode: 'light',
  surfacePreset: 'slate-clean',
  brandPalette: 'blue',
  sidebarCollapseMode: 'mini-rail',
  isDesktopSidebarCollapsed: false,
};

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);

  readonly isDarkMode = signal<boolean>(false);
  readonly themeMode = computed<'light' | 'dark'>(() => (this.isDarkMode() ? 'dark' : 'light'));
  readonly surfacePreset = signal<SurfacePreset>(DEFAULT_PREFERENCES.surfacePreset);
  readonly brandPalette = signal<BrandPalette>(DEFAULT_PREFERENCES.brandPalette);
  readonly sidebarCollapseMode = signal<SidebarCollapseMode>(DEFAULT_PREFERENCES.sidebarCollapseMode);
  readonly isDesktopSidebarCollapsed = signal<boolean>(DEFAULT_PREFERENCES.isDesktopSidebarCollapsed);

  constructor() {
    if (this.isBrowser()) {
      this.hydrateFromStorage();
      this.applyDomAttributes();
    }

    effect(() => {
      const dark = this.isDarkMode();
      const surface = this.surfacePreset();
      const brand = this.brandPalette();
      const collapseMode = this.sidebarCollapseMode();
      const collapsed = this.isDesktopSidebarCollapsed();

      if (this.isBrowser()) {
        this.applyDomAttributes(dark, surface, brand);
        this.persistPreferences({
          themeMode: dark ? 'dark' : 'light',
          surfacePreset: surface,
          brandPalette: brand,
          sidebarCollapseMode: collapseMode,
          isDesktopSidebarCollapsed: collapsed,
        });
      }
    });
  }

  toggleTheme(): void {
    this.setTheme(!this.isDarkMode());
  }

  setTheme(dark: boolean): void {
    this.isDarkMode.set(dark);
    if (this.isBrowser()) {
      this.applyDomAttributes();
      this.persistPreferences();
    }
  }

  setSurfacePreset(preset: SurfacePreset): void {
    this.surfacePreset.set(preset);
    if (this.isBrowser()) {
      this.applyDomAttributes();
      this.persistPreferences();
    }
  }

  setBrandPalette(palette: BrandPalette): void {
    this.brandPalette.set(palette);
    if (this.isBrowser()) {
      this.applyDomAttributes();
      this.persistPreferences();
    }
  }

  setSidebarCollapseMode(mode: SidebarCollapseMode): void {
    this.sidebarCollapseMode.set(mode);
    if (this.isBrowser()) {
      this.persistPreferences();
    }
  }

  toggleDesktopSidebar(): void {
    this.setDesktopSidebarCollapsed(!this.isDesktopSidebarCollapsed());
  }

  setDesktopSidebarCollapsed(collapsed: boolean): void {
    this.isDesktopSidebarCollapsed.set(collapsed);
    if (this.isBrowser()) {
      this.persistPreferences();
    }
  }

  /**
   * Aplica valores predeterminados corporativos (p. ej. desde configuración del Tenant)
   * únicamente si el operador no tiene una preferencia personalizada activa en localStorage,
   * o si se fuerza mediante force = true.
   */
  applyCompanyDefaults(defaults: Partial<UiThemePreferences>, force = false): void {
    if (!force && this.isBrowser() && localStorage.getItem(UI_PREFERENCES_STORAGE_KEY)) {
      return;
    }
    if (defaults.themeMode !== undefined) {
      this.isDarkMode.set(defaults.themeMode === 'dark');
    }
    if (defaults.surfacePreset !== undefined) {
      this.surfacePreset.set(defaults.surfacePreset);
    }
    if (defaults.brandPalette !== undefined) {
      this.brandPalette.set(defaults.brandPalette);
    }
    if (defaults.sidebarCollapseMode !== undefined) {
      this.sidebarCollapseMode.set(defaults.sidebarCollapseMode);
    }
    if (this.isBrowser()) {
      this.applyDomAttributes();
      this.persistPreferences();
    }
  }

  resetToDefaults(): void {
    this.isDarkMode.set(false);
    this.surfacePreset.set(DEFAULT_PREFERENCES.surfacePreset);
    this.brandPalette.set(DEFAULT_PREFERENCES.brandPalette);
    this.sidebarCollapseMode.set(DEFAULT_PREFERENCES.sidebarCollapseMode);
    this.isDesktopSidebarCollapsed.set(false);
    if (this.isBrowser()) {
      this.applyDomAttributes();
      this.persistPreferences();
    }
  }

  private hydrateFromStorage(): void {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    const rawPrefs = localStorage.getItem(UI_PREFERENCES_STORAGE_KEY);

    if (rawPrefs) {
      try {
        const parsed = JSON.parse(rawPrefs) as Partial<UiThemePreferences>;
        if (parsed.themeMode === 'dark' || parsed.themeMode === 'light') {
          this.isDarkMode.set(parsed.themeMode === 'dark');
        } else if (savedTheme !== null) {
          this.isDarkMode.set(savedTheme === 'dark');
        }
        if (parsed.surfacePreset && ['slate-clean', 'warm-tinted', 'dark-nav'].includes(parsed.surfacePreset)) {
          this.surfacePreset.set(parsed.surfacePreset);
        }
        if (parsed.brandPalette && ['blue', 'aqua', 'purple', 'emerald', 'orange'].includes(parsed.brandPalette)) {
          this.brandPalette.set(parsed.brandPalette);
        }
        if (parsed.sidebarCollapseMode && ['mini-rail', 'full-hide'].includes(parsed.sidebarCollapseMode)) {
          this.sidebarCollapseMode.set(parsed.sidebarCollapseMode);
        }
        if (typeof parsed.isDesktopSidebarCollapsed === 'boolean') {
          this.isDesktopSidebarCollapsed.set(parsed.isDesktopSidebarCollapsed);
        }
        return;
      } catch {
        // Fallback a clave simple si el JSON está corrupto
      }
    }

    const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    const initialDark = savedTheme !== null ? savedTheme === 'dark' : prefersDark;
    this.isDarkMode.set(initialDark);
  }

  private applyDomAttributes(
    dark = this.isDarkMode(),
    surface = this.surfacePreset(),
    brand = this.brandPalette()
  ): void {
    if (!this.isBrowser()) return;
    const root = document.documentElement;
    if (dark) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    root.setAttribute('data-surface', surface);
    root.setAttribute('data-brand', brand);
  }

  private persistPreferences(prefs?: UiThemePreferences): void {
    if (!this.isBrowser()) return;
    const current: UiThemePreferences = prefs ?? {
      themeMode: this.isDarkMode() ? 'dark' : 'light',
      surfacePreset: this.surfacePreset(),
      brandPalette: this.brandPalette(),
      sidebarCollapseMode: this.sidebarCollapseMode(),
      isDesktopSidebarCollapsed: this.isDesktopSidebarCollapsed(),
    };
    localStorage.setItem(THEME_STORAGE_KEY, current.themeMode);
    localStorage.setItem(UI_PREFERENCES_STORAGE_KEY, JSON.stringify(current));
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof document !== 'undefined';
  }
}
