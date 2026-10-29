import { TestBed } from '@angular/core/testing';
import { ThemeService, THEME_STORAGE_KEY, UI_PREFERENCES_STORAGE_KEY } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    document.documentElement.removeAttribute('data-surface');
    document.documentElement.removeAttribute('data-brand');
    TestBed.configureTestingModule({
      providers: [ThemeService],
    });
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    document.documentElement.removeAttribute('data-surface');
    document.documentElement.removeAttribute('data-brand');
  });

  it('should be created with default TailwindAdmin v3.0 preferences', () => {
    expect(service).toBeTruthy();
    expect(service.surfacePreset()).toBe('slate-clean');
    expect(service.brandPalette()).toBe('blue');
    expect(service.sidebarCollapseMode()).toBe('mini-rail');
    expect(document.documentElement.getAttribute('data-surface')).toBe('slate-clean');
    expect(document.documentElement.getAttribute('data-brand')).toBe('blue');
  });

  it('should initialize and toggle theme from light to dark', () => {
    service.setTheme(false);
    expect(service.isDarkMode()).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');

    service.toggleTheme();
    expect(service.isDarkMode()).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });

  it('should toggle theme from dark to light', () => {
    service.setTheme(true);
    expect(service.isDarkMode()).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    service.toggleTheme();
    expect(service.isDarkMode()).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });

  it('should hot-swap surface preset and brand palette on <html> and persist in localStorage', () => {
    service.setSurfacePreset('warm-tinted');
    expect(service.surfacePreset()).toBe('warm-tinted');
    expect(document.documentElement.getAttribute('data-surface')).toBe('warm-tinted');

    service.setBrandPalette('purple');
    expect(service.brandPalette()).toBe('purple');
    expect(document.documentElement.getAttribute('data-brand')).toBe('purple');

    const stored = JSON.parse(localStorage.getItem(UI_PREFERENCES_STORAGE_KEY) || '{}');
    expect(stored.surfacePreset).toBe('warm-tinted');
    expect(stored.brandPalette).toBe('purple');
  });

  it('should toggle desktop sidebar collapse state and mode', () => {
    expect(service.isDesktopSidebarCollapsed()).toBe(false);
    service.toggleDesktopSidebar();
    expect(service.isDesktopSidebarCollapsed()).toBe(true);

    service.setSidebarCollapseMode('full-hide');
    expect(service.sidebarCollapseMode()).toBe('full-hide');
  });

  it('should reset all preferences to defaults', () => {
    service.setTheme(true);
    service.setSurfacePreset('dark-nav');
    service.setBrandPalette('orange');
    service.setSidebarCollapseMode('full-hide');
    service.setDesktopSidebarCollapsed(true);

    service.resetToDefaults();
    expect(service.isDarkMode()).toBe(false);
    expect(service.surfacePreset()).toBe('slate-clean');
    expect(service.brandPalette()).toBe('blue');
    expect(service.sidebarCollapseMode()).toBe('mini-rail');
    expect(service.isDesktopSidebarCollapsed()).toBe(false);
  });
});
