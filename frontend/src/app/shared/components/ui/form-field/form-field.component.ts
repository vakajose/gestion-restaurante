import { Component, computed, input } from '@angular/core';
import { IconComponent } from '../icon/icon.component';

/**
 * Wrapper de Campo de Formulario con Validación Inline (OBS-UI-07, OBS-UI-09 / ADR-0014).
 * Cuando [error] contiene un mensaje, aplica automáticamente la clase .has-error
 * que tiñe el borde del input/select/textarea hijo en rojo y muestra el motivo debajo.
 */
@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div [class]="wrapperClasses()" [attr.data-field-id]="fieldId() || null">
      @if (label()) {
        <label
          [attr.for]="fieldId() || null"
          class="block text-xs font-semibold uppercase tracking-wider text-link dark:text-darklink mb-1.5"
        >
          {{ label() }}
          @if (required()) {
            <span class="text-error ml-0.5" title="Campo obligatorio">*</span>
          }
        </label>
      }

      <div class="relative">
        <ng-content></ng-content>
      </div>

      @if (hasError()) {
        <p
          data-testid="form-field-error"
          role="alert"
          class="mt-1.5 text-xs font-medium text-[#be123c] dark:text-rose-400 flex items-center gap-1.5"
        >
          <app-icon name="danger-triangle" size="xs"></app-icon>
          <span>{{ error() }}</span>
        </p>
      } @else if (hint()) {
        <p class="mt-1 text-xs text-bodytext/80 dark:text-darklink/80">
          {{ hint() }}
        </p>
      }
    </div>
  `,
})
export class FormFieldComponent {
  readonly label = input<string>('');
  readonly fieldId = input<string>('');
  readonly required = input<boolean>(false);
  readonly error = input<string | null | undefined>(null);
  readonly hint = input<string>('');

  readonly hasError = computed<boolean>(() => Boolean(this.error()?.trim()));

  readonly wrapperClasses = computed<string>(() =>
    this.hasError() ? 'w-full has-error' : 'w-full'
  );
}
