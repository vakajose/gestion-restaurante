import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent, SolarIconName } from '../icon/icon.component';

export interface BreadcrumbItem {
  label: string;
  link?: string;
}

/**
 * Contenedor canónico de vista (OBS-UI-04 / ADR-0014).
 * Estandariza márgenes, ancho máximo (1536px o fluid), breadcrumbs y cabecera con slot de acciones.
 */
@Component({
  selector: 'app-page-wrapper',
  standalone: true,
  imports: [RouterLink, IconComponent],
  template: `
    <div [class]="containerClasses()">
      @if (title() || breadcrumbs().length > 0) {
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div class="min-w-0">
            @if (breadcrumbs().length > 0) {
              <nav class="flex items-center text-xs text-bodytext dark:text-darklink mb-1.5 gap-1.5" aria-label="Breadcrumb">
                @for (crumb of breadcrumbs(); track crumb.label; let last = $last) {
                  @if (crumb.link && !last) {
                    <a [routerLink]="crumb.link" class="hover:text-primary transition-colors">{{ crumb.label }}</a>
                  } @else {
                    <span [class.font-semibold]="last" [class.text-link]="last" [class.dark:text-white]="last">
                      {{ crumb.label }}
                    </span>
                  }
                  @if (!last) {
                    <span aria-hidden="true" class="opacity-50">/</span>
                  }
                }
              </nav>
            }

            @if (title()) {
              <h1 class="text-xl sm:text-2xl font-bold text-link dark:text-white flex items-center gap-2.5 tracking-tight">
                @if (icon()) {
                  <span class="w-8 h-8 rounded-md bg-lightprimary text-primary inline-flex items-center justify-center shrink-0">
                    <app-icon [name]="icon()!" size="md"></app-icon>
                  </span>
                } @else if (emoji()) {
                  <span aria-hidden="true">{{ emoji() }}</span>
                }
                <span class="truncate">{{ title() }}</span>
              </h1>
            }

            @if (subtitle()) {
              <p class="text-sm text-bodytext dark:text-darklink mt-1">
                {{ subtitle() }}
              </p>
            }
          </div>

          <div class="flex flex-wrap items-center gap-2.5 shrink-0">
            <ng-content select="[pageActions]"></ng-content>
          </div>
        </div>
      }

      <ng-content></ng-content>
    </div>
  `,
})
export class PageWrapperComponent {
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly icon = input<SolarIconName | undefined>(undefined);
  readonly emoji = input<string>('');
  readonly breadcrumbs = input<BreadcrumbItem[]>([]);
  readonly fluid = input<boolean>(false);

  readonly containerClasses = computed<string>(() => {
    const base = 'space-y-6 w-full';
    return this.fluid() ? base : `${base} max-w-[1536px] mx-auto`;
  });
}
