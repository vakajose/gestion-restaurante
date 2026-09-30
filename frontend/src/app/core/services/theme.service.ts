import { Injectable, signal, effect, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export const THEME_STORAGE_KEY = 'app_theme';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  readonly isDarkMode = signal<boolean>(false);

  constructor() {
    if (this.isBrowser()) {
      const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
      const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
      const initialDark = savedTheme !== null ? savedTheme === 'dark' : prefersDark;
      this.isDarkMode.set(initialDark);
      this.applyTheme(initialDark);
    }

    // Sincronización reactiva con Signals
    effect(() => {
      const dark = this.isDarkMode();
      if (this.isBrowser()) {
        this.applyTheme(dark);
        localStorage.setItem(THEME_STORAGE_KEY, dark ? 'dark' : 'light');
      }
    });
  }

  toggleTheme(): void {
    const next = !this.isDarkMode();
    this.isDarkMode.set(next);
    if (this.isBrowser()) {
      this.applyTheme(next);
      localStorage.setItem(THEME_STORAGE_KEY, next ? 'dark' : 'light');
    }
  }

  setTheme(dark: boolean): void {
    this.isDarkMode.set(dark);
    if (this.isBrowser()) {
      this.applyTheme(dark);
      localStorage.setItem(THEME_STORAGE_KEY, dark ? 'dark' : 'light');
    }
  }

  private applyTheme(dark: boolean): void {
    if (!this.isBrowser()) return;
    if (dark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof document !== 'undefined';
  }
}
