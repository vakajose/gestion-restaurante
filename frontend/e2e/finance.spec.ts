import { test, expect } from '@playwright/test';
import { AuthPage } from './pages/auth.page';
import { FinancePage } from './pages/finance.page';
import { CashControlPage } from './pages/cash-control.page';

test.describe('Finanzas, Arqueo de Caja y Gastos Menores (TailwindAdmin)', () => {
  let authPage: AuthPage;
  let financePage: FinancePage;
  let cashControlPage: CashControlPage;

  test.beforeEach(async ({ page }) => {
    authPage = new AuthPage(page);
    financePage = new FinancePage(page);
    cashControlPage = new CashControlPage(page);

    // Autenticar previamente con Admin
    await authPage.loginAsAdmin();
  });

  test('debe navegar a Finanzas y mostrar los KPIs de turno y arqueo', async ({ page }) => {
    await financePage.goto();
    await expect(page.locator('h1')).toContainText('Finanzas y Arqueos');

    // Tarjetas KPI ejecutivas de Finanzas
    await expect(page.locator('text=Turnos Registrados')).toBeVisible();
    await expect(page.locator('text=Ventas Históricas')).toBeVisible();
    await expect(page.locator('text=Gastos Aprobados')).toBeVisible();
    await expect(page.locator('text=Discrepancia Neta')).toBeVisible();

    // Pestañas
    await expect(page.locator('[data-testid="tab-shifts"]')).toBeVisible();
    await expect(page.locator('[data-testid="tab-expenses"]')).toBeVisible();
  });

  test('debe listar el historial de turnos y permitir filtrar por estado', async ({ page }) => {
    await financePage.goto();
    await financePage.selectTab('shifts');

    // Filtros de estado
    await expect(page.locator('button:has-text("Abiertos"), [data-testid="filter-shifts-open"]')).toBeVisible();
    await expect(page.locator('button:has-text("Pendientes de Consolidar"), [data-testid="filter-shifts-closed"]')).toBeVisible();
    await expect(page.locator('button:has-text("Consolidados"), [data-testid="filter-shifts-consolidated"]')).toBeVisible();

    // Tabla de turnos visible
    await expect(page.locator('table').first()).toBeVisible({ timeout: 10000 });
  });

  test('debe alternar a pestaña de gastos y auditar egresos', async ({ page }) => {
    await financePage.goto();
    await financePage.selectTab('expenses');

    // Tabla de egresos o estado vacío
    await expect(page.locator('table').or(page.locator('text=Sin egresos registrados')).first()).toBeVisible({ timeout: 10000 });
  });

  test('debe acceder a Control de Caja chica y visualizar arqueo en vivo', async ({ page }) => {
    await cashControlPage.goto();
    await expect(page.locator('h1')).toContainText('Control de Caja');

    // KPIs de gaveta operativa
    await expect(page.locator('text=Efectivo en Gaveta')).toBeVisible();
    await expect(page.locator('text=Ventas Digitales')).toBeVisible();
    await expect(page.locator('text=Total Ventas Turno')).toBeVisible();
    await expect(page.locator('text=Gastos Aprobados')).toBeVisible();
  });
});
