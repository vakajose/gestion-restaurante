import { test, expect } from '@playwright/test';

test.describe('Finanzas, Arqueo de Caja y Gastos Menores (TailwindAdmin)', () => {
  test.beforeEach(async ({ page }) => {
    // Autenticar previamente con Admin
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });
  });

  test('debe navegar a Finanzas y mostrar los KPIs de turno y arqueo', async ({ page }) => {
    await page.goto('/finance');
    await expect(page.locator('h1')).toContainText('Finanzas y Arqueo de Caja Chica');

    // Tarjetas KPI ejecutivas
    await expect(page.locator('text=Estado de Caja')).toBeVisible();
    await expect(page.locator('text=Efectivo Teórico').first()).toBeVisible();
    await expect(page.locator('text=Ventas Turno')).toBeVisible();
    await expect(page.locator('text=Gastos Aprobados')).toBeVisible();

    // Pestañas
    await expect(page.locator('[data-testid="tab-shifts"]')).toBeVisible();
    await expect(page.locator('[data-testid="tab-expenses"]')).toBeVisible();
  });

  test('debe mostrar el estado del turno actual y desglose de arqueo', async ({ page }) => {
    await page.goto('/finance');
    await expect(page.locator('h1')).toBeVisible();

    const turnoActivo = page.locator('text=Turno Activo en Curso');
    const sinTurno = page.locator('text=No hay un turno de caja abierto');
    await expect(turnoActivo.or(sinTurno)).toBeVisible({ timeout: 10000 });

    if (await turnoActivo.isVisible()) {
      await expect(page.locator('text=Saldo Inicial de Caja')).toBeVisible();
      await expect(page.locator('text=(+) Ventas en Efectivo')).toBeVisible();
      await expect(page.locator('text=(-) Gastos Caja Chica')).toBeVisible();
      await expect(page.locator('text=(=) Efectivo Teórico en Gaveta')).toBeVisible();
    } else {
      await expect(page.locator('[data-testid="btn-open-shift-cta"]')).toBeVisible();
    }
  });

  test('debe alternar a pestaña de gastos y registrar un nuevo gasto', async ({ page }) => {
    await page.goto('/finance');

    const expensesTab = page.locator('[data-testid="tab-expenses"]');
    await expensesTab.click();

    // Registrar nuevo gasto
    const newExpenseBtn = page.locator('[data-testid="btn-add-expense-tab"]');
    await expect(newExpenseBtn).toBeVisible({ timeout: 10000 });
    await newExpenseBtn.click();

    // Modal de nuevo gasto visible
    await expect(page.locator('h3:has-text("Registrar Gasto de Caja Menor")')).toBeVisible();

    const uniqueDesc = 'Compra insumos E2E ' + Date.now();
    await page.locator('[data-testid="input-expense-desc"]').fill(uniqueDesc);
    await page.locator('[data-testid="input-expense-amount"]').fill('22.50');

    // Guardar gasto
    await page.locator('[data-testid="btn-confirm-save-expense"]').click();

    // Alerta de éxito
    await expect(page.locator('text=Gasto registrado correctamente.')).toBeVisible({ timeout: 10000 });

    // Verificar presencia en la tabla de gastos
    await expect(page.locator(`text=${uniqueDesc}`).first()).toBeVisible();
  });

  test('debe interactuar con el modal de arqueo y calcular diferencia en tiempo real', async ({ page }) => {
    await page.goto('/finance');

    // Esperar a que alguno de los botones de turno esté listo
    const closeBtn = page.locator('[data-testid="btn-close-shift"]');
    const openBtn = page.locator('[data-testid="btn-open-shift"]');
    await expect(closeBtn.or(openBtn)).toBeVisible({ timeout: 10000 });

    // Si no hay turno abierto, abrir uno primero para probar el arqueo
    if (await openBtn.isVisible()) {
      await openBtn.click();
      await page.locator('[data-testid="input-initial-cash"]').fill('100.00');
      await page.locator('[data-testid="btn-confirm-open-shift"]').click();
      await expect(page.locator('text=Turno de caja abierto exitosamente.')).toBeVisible({ timeout: 10000 });
    }

    // Abrir modal de arqueo y cierre
    await expect(closeBtn).toBeVisible({ timeout: 10000 });
    await closeBtn.click();

    await expect(page.locator('h3:has-text("Arqueo y Cierre de Caja")')).toBeVisible();

    // Modificar conteo real y verificar que calcula resultado dinámicamente
    const actualInput = page.locator('[data-testid="input-actual-cash"]');
    await actualInput.fill('500.00');
    await expect(page.locator('text=Resultado del Arqueo:')).toBeVisible();

    // Cancelar cierre
    await page.locator('button:has-text("Cancelar")').last().click();
    await expect(page.locator('h3:has-text("Arqueo y Cierre de Caja")')).not.toBeVisible();
  });
});
