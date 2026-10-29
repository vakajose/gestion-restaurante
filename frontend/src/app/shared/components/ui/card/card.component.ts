import { Component, computed, input } from '@angular/core';

/**
 * Tarjeta Estandarizada TailwindAdmin v3.0 (OBS-UI-01 / ADR-0014).
 * Aplica superficie --bg-surface, borde nítido --border-default y sombra multicapa --theme-shadow-md.
 */
@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div class="card overflow-hidden">
      @if (title() || subtitle()) {
        <div class="px-5 py-4 sm:px-6 border-b border-border flex items-center justify-between gap-4">
          <div class="min-w-0">
            @if (title()) {
              <h3 class="card-title truncate">{{ title() }}</h3>
            }
            @if (subtitle()) {
              <p class="card-subtitle mt-0.5">{{ subtitle() }}</p>
            }
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <ng-content select="[cardHeaderActions]"></ng-content>
          </div>
        </div>
      }

      <div [class]="bodyClasses()">
        <ng-content></ng-content>
      </div>

      <ng-content select="[cardFooter]"></ng-content>
    </div>
  `,
})
export class CardComponent {
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly noPadding = input<boolean>(false);

  readonly bodyClasses = computed<string>(() => (this.noPadding() ? 'p-0' : 'card-body'));
}
