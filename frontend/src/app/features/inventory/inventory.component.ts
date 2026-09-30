import { Component } from '@angular/core';

@Component({
  selector: 'app-inventory',
  standalone: true,
  template: `
    <div class="rounded-lg p-6 bg-card-light dark:bg-card-dark border border-gray-100 dark:border-card-border shadow-sm">
      <h2 class="text-2xl font-bold text-slate-800 dark:text-white">Inventario y Kardex</h2>
      <p class="text-gray-500 dark:text-gray-400 mt-2">
        Deducción atómica de recetas ante OrderPaidEvent y control de stock por sucursal.
      </p>
    </div>
  `,
})
export class InventoryComponent {}
