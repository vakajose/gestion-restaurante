import { Component } from '@angular/core';

@Component({
  selector: 'app-pos',
  standalone: true,
  template: `
    <div class="card">
      <div class="card-body">
        <div class="flex items-center justify-between mb-4">
          <h2 class="card-title text-xl">Terminal Punto de Venta (POS)</h2>
          <span class="bg-lightsuccess text-success text-xs font-semibold px-2.5 py-1 rounded-sm">
            Offline First Ready
          </span>
        </div>
        <p class="card-subtitle">
          Comandas rápidas, persistencia local con Dexie.js y reconciliación en segundo plano.
        </p>
      </div>
    </div>
  `,
})
export class PosComponent {}
