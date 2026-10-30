import { Routes } from '@angular/router';
import { authGuard, guestGuard, permissionGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'pos',
  },
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'onboarding',
    loadComponent: () =>
      import('./features/onboarding/onboarding.component').then((m) => m.OnboardingComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'auth',
    pathMatch: 'full',
    redirectTo: 'auth/login',
  },
  {
    path: 'pos',
    loadComponent: () =>
      import('./features/pos/pos.component').then((m) => m.PosComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['pos:access'] },
  },
  {
    path: 'catalog/menus',
    loadComponent: () =>
      import('./features/catalog/menus/daily-menus.component').then((m) => m.DailyMenusComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['catalog:read'] },
  },
  {
    path: 'catalog',
    loadComponent: () =>
      import('./features/catalog/catalog.component').then((m) => m.CatalogComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['catalog:read'] },
  },
  {
    path: 'inventory',
    loadComponent: () =>
      import('./features/inventory/inventory.component').then((m) => m.InventoryComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['inventory:read'] },
  },
  {
    path: 'finance',
    loadComponent: () =>
      import('./features/finance/finance.component').then((m) => m.FinanceComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['finance:shifts'] },
  },
  {
    path: 'company',
    loadComponent: () =>
      import('./features/company/company.component').then((m) => m.CompanyComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['company:read'] },
  },
  {
    path: 'users/roles',
    loadComponent: () =>
      import('./features/users/roles/roles.component').then((m) => m.RolesComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['users:read'] },
  },
  {
    path: 'settings/roles',
    loadComponent: () =>
      import('./features/users/roles/roles.component').then((m) => m.RolesComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['users:read'] },
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./features/users/users.component').then((m) => m.UsersComponent),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['users:read'] },
  },
  {
    path: 'settings/parameters',
    loadComponent: () =>
      import('./features/settings/parameters/parameters.component').then(
        (m) => m.ParametersComponent
      ),
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['settings:read'] },
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: 'pos',
  },
];
