import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Catálogo oficial de vectores SVG de Solar Icons (CC BY 4.0) y Tabler Icons (MIT),
 * empaquetado localmente para garantizar 0 llamadas de red en modo Offline-First (ADR-0014).
 */
export const SOLAR_ICON_REGISTRY = {
  'hamburger-menu': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.75"><path d="M20 7L4 7"/><path d="M20 12L4 12"/><path d="M20 17L4 17"/></g>`,
  'sidebar-minimalistic': `<g fill="none" stroke="currentColor" stroke-width="1.6"><rect width="18" height="16" x="3" y="4" rx="3"/><path stroke-linecap="round" d="M9 4v16"/></g>`,
  'pos-terminal': `<g fill="none" stroke="currentColor" stroke-width="1.6"><rect width="18" height="14" x="3" y="5" rx="3"/><path stroke-linecap="round" d="M3 10h18M7 15h3"/></g>`,
  'box-minimalistic': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="m12 3l8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m12 12l8-4.5M12 12v9M12 12L4 7.5m12-2.25l-8 4.5"/></g>`,
  'wallet-money': `<g fill="none" stroke="currentColor" stroke-width="1.6"><rect width="18" height="14" x="3" y="6" rx="3"/><path stroke-linecap="round" d="M16 13h2"/><path d="M6 6V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1"/></g>`,
  'chef-hat': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M6 17h12v3a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-3Zm0 0V13a4 4 0 0 1-.8-7.9A5 5 0 0 1 12 3a5 5 0 0 1 6.8 2.1A4 4 0 0 1 18 13v4"/><path d="M9 17v-3m3 3v-4m3 4v-3"/></g>`,
  'buildings-2': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M3 21h18M5 21V6a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v15"/><path d="M15 10h2a2 2 0 0 1 2 2v9M9 8h2m-2 4h2m-2 4h2"/></g>`,
  'users-group': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.6"><circle cx="9" cy="8" r="3.5"/><path d="M3 19.5c0-2.8 2.7-5 6-5s6 2.2 6 5"/><circle cx="17" cy="9" r="2.5"/><path d="M21 19.5c0-2-1.8-3.7-4-4"/></g>`,
  'shield-user': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M12 3l7 3v5c0 5-3.5 8.5-7 10c-3.5-1.5-7-5-7-10V6l7-3Z"/><circle cx="12" cy="10" r="2"/><path d="M9 15.5c.8-1.2 1.8-1.8 3-1.8s2.2.6 3 1.8"/></g>`,
  'settings': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33a1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.604.852.997 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"/></g>`,
  'sun': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.6"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></g>`,
  'moon': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M21 12.79A9 9 0 1 1 11.21 3A7 7 0 0 0 21 12.79Z"/></g>`,
  'palette': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M12 2a10 10 0 0 0 0 20a2.5 2.5 0 0 0 2.5-2.5c0-.6-.23-1.15-.6-1.57c-.36-.4-.58-.94-.58-1.53A2.4 2.4 0 0 1 15.72 14H17a5 5 0 0 0 5-5c0-3.87-4.48-7-10-7Z"/><circle cx="7.5" cy="10.5" r="1" fill="currentColor"/><circle cx="10.5" cy="7.5" r="1" fill="currentColor"/><circle cx="14.5" cy="7.5" r="1" fill="currentColor"/></g>`,
  'check-circle': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><circle cx="12" cy="12" r="9"/><path d="m8.5 12.5l2.3 2.3l4.7-5.1"/></g>`,
  'danger-triangle': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z"/><path d="M12 9v4m0 4h.01"/></g>`,
  'info-circle': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><circle cx="12" cy="12" r="9"/><path d="M12 16v-4m0-4h.01"/></g>`,
  'close-circle': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><circle cx="12" cy="12" r="9"/><path d="m15 9l-6 6m0-6l6 6"/></g>`,
  'close': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"><path d="M18 6L6 18M6 6l12 12"/></g>`,
  'plus': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"><path d="M12 5v14m-7-7h14"/></g>`,
  'trash-bin': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M3 6h18m-2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m-6 5v6m4-6v6"/></g>`,
  'pen-new-square': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1l1-4l9.5-9.5Z"/></g>`,
  'refresh': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M21 12a9 9 0 0 0-9-9a9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5m-5 4a9 9 0 0 0 9 9a9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></g>`,
  'magnifer': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.6"><circle cx="11" cy="11" r="7"/><path d="m20 20l-3.5-3.5"/></g>`,
  'alt-arrow-down': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><path d="m6 9l6 6l6-6"/></g>`,
  'alt-arrow-right': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"><path d="m9 18l6-6l-6-6"/></g>`,
  'logout-2': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4m7 14l5-5l-5-5m5 5H9"/></g>`,
  'academic-cap': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="m22 10l-10-5L2 10l10 5l10-5v6"/><path d="M6 12v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5"/></g>`,
  'lock': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></g>`,
  'unlock': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></g>`,
  'chart-square': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><rect width="18" height="18" x="3" y="3" rx="3"/><path d="M8 17v-4m4 4v-8m4 8v-5"/></g>`,
  'receipt-list': `<g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6"><path d="M4 4v16l2.5-1.5L9 20l2.5-1.5L14 20l2.5-1.5L19 20V4a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1Z"/><path d="M8 8h8m-8 4h8m-8 4h5"/></g>`,
} as const;

export type SolarIconName = keyof typeof SOLAR_ICON_REGISTRY;

export type IconSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-icon',
  standalone: true,
  host: {
    class: 'inline-flex items-center justify-center shrink-0 select-none',
    '[attr.aria-hidden]': '"true"',
  },
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      [class]="sizeClass()"
      [innerHTML]="safeSvg()"
    ></svg>
  `,
})
export class IconComponent {
  private readonly sanitizer = inject(DomSanitizer);

  readonly name = input.required<SolarIconName>();
  readonly size = input<IconSize>('md');

  readonly sizeClass = computed<string>(() => {
    switch (this.size()) {
      case 'xs':
        return 'w-3.5 h-3.5';
      case 'sm':
        return 'w-4 h-4';
      case 'lg':
        return 'w-6 h-6';
      case 'xl':
        return 'w-7 h-7';
      case 'md':
      default:
        return 'w-5 h-5';
    }
  });

  readonly safeSvg = computed<SafeHtml>(() => {
    const raw = SOLAR_ICON_REGISTRY[this.name()] ?? SOLAR_ICON_REGISTRY['info-circle'];
    return this.sanitizer.bypassSecurityTrustHtml(raw);
  });
}
