import { Component } from '@angular/core';

@Component({
  selector: 'app-inventory',
  standalone: true,
  template: `
    <div class="card">
      <div class="card-body">
        <h2 class="card-title text-xl">Inventario y Kardex</h2>
        <p class="card-subtitle mt-2">
          Deducción atómica de recetas ante OrderPaidEvent y control de stock por sucursal.
        </p>
      </div>
    </div>
  `,
})
export class InventoryComponent {}
