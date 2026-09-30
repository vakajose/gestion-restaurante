import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'pos',
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/auth.component').then((m) => m.AuthComponent),
  },
  {
    path: 'pos',
    loadComponent: () =>
      import('./features/pos/pos.component').then((m) => m.PosComponent),
  },
  {
    path: 'inventory',
    loadComponent: () =>
      import('./features/inventory/inventory.component').then((m) => m.InventoryComponent),
  },
  {
    path: 'finance',
    loadComponent: () =>
      import('./features/finance/finance.component').then((m) => m.FinanceComponent),
  },
  {
    path: '**',
    redirectTo: 'pos',
  },
];
