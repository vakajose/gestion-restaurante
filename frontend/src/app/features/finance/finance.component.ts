import { Component } from '@angular/core';

@Component({
  selector: 'app-finance',
  standalone: true,
  template: `
    <div class="card">
      <div class="card-body">
        <h2 class="card-title text-xl">Finanzas y Caja Chica</h2>
        <p class="card-subtitle mt-2">
          Apertura/cierre de turnos de caja, arqueo de efectivo y workflow de gastos menores.
        </p>
      </div>
    </div>
  `,
})
export class FinanceComponent {}
