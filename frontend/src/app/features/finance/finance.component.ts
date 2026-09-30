import { Component } from '@angular/core';

@Component({
  selector: 'app-finance',
  standalone: true,
  template: `
    <div class="rounded-lg p-6 bg-card-light dark:bg-card-dark border border-gray-100 dark:border-card-border shadow-sm">
      <h2 class="text-2xl font-bold text-slate-800 dark:text-white">Finanzas y Caja Chica</h2>
      <p class="text-gray-500 dark:text-gray-400 mt-2">
        Apertura/cierre de turnos de caja, arqueo de efectivo y workflow de gastos menores.
      </p>
    </div>
  `,
})
export class FinanceComponent {}
