import { Component } from '@angular/core';

@Component({
  selector: 'app-auth',
  standalone: true,
  template: `
    <div class="relative overflow-hidden rounded-lg p-8 bg-card-light dark:bg-card-dark border border-gray-100 dark:border-card-border shadow-sm">
      <!-- Círculos decorativos Auth según especificación Datta Able -->
      <div class="absolute -top-12 -right-12 w-48 h-48 rounded-full bg-auth-turquoise/20 blur-xl pointer-events-none"></div>
      <div class="absolute -bottom-12 -left-12 w-48 h-48 rounded-full bg-auth-purple/20 blur-xl pointer-events-none"></div>

      <div class="relative z-10">
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white">Módulo de Autenticación</h2>
        <p class="text-gray-500 dark:text-gray-400 mt-2">
          Login adaptable (usuario/correo) y registro con estética Datta Able.
        </p>
      </div>
    </div>
  `,
})
export class AuthComponent {}
