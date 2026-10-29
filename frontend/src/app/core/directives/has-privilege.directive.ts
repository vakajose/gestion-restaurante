import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  effect,
  inject,
  signal,
} from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Directiva estructural Zoneless que renderiza condicionalmente un bloque de plantilla
 * si el usuario autenticado posee el privilegio (o alguno de los privilegios) especificados.
 *
 * Uso:
 *   <button *hasPrivilege="'pos:discount_apply'">Aplicar Descuento</button>
 *   <div *appHasPrivilege="['users:write', 'roles:manage']">...</div>
 */
@Directive({
  selector: '[appHasPrivilege], [hasPrivilege]',
  standalone: true,
})
export class HasPrivilegeDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authService = inject(AuthService);

  private readonly requiredPrivilege = signal<string | string[]>('');
  private isViewCreated = false;

  @Input()
  set appHasPrivilege(value: string | string[]) {
    this.requiredPrivilege.set(value);
  }

  @Input()
  set hasPrivilege(value: string | string[]) {
    this.requiredPrivilege.set(value);
  }

  constructor() {
    effect(() => {
      // Suscribirse reactivamente tanto al usuario actual como al privilegio requerido
      this.authService.currentUser();
      const priv = this.requiredPrivilege();
      const allowed = this.authService.hasPrivilege(priv);

      if (allowed && !this.isViewCreated) {
        this.viewContainer.createEmbeddedView(this.templateRef);
        this.isViewCreated = true;
      } else if (!allowed && this.isViewCreated) {
        this.viewContainer.clear();
        this.isViewCreated = false;
      }
    });
  }
}
