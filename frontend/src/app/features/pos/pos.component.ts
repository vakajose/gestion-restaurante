import { Component } from '@angular/core';

@Component({
  selector: 'app-pos',
  standalone: true,
  template: `
    <div class="rounded-lg p-6 bg-card-light dark:bg-card-dark border border-gray-100 dark:border-card-border shadow-sm">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white">Terminal Punto de Venta (POS)</h2>
        <span class="bg-success text-white text-xs font-semibold px-2.5 py-1 rounded">
          Offline First Ready
        </span>
      </div>
      <p class="text-gray-500 dark:text-gray-400">
        Comandas rápidas, persistencia local con Dexie.js y reconciliación en segundo plano.
      </p>
    </div>
  `,
})
export class PosComponent {}
