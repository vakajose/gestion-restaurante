import { test, expect } from '@playwright/test';

test.describe('Inventario Físico y Kardex (TailwindAdmin)', () => {
  test.beforeEach(async ({ page }) => {
    // Autenticar previamente con Admin
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });
  });

  test('debe navegar a Inventario y mostrar los KPIs ejecutivos', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page.locator('h1')).toContainText('Inventario Físico y Kardex');

    // Verificar tarjetas KPI
    await expect(page.locator('text=Insumos Registrados')).toBeVisible();
    await expect(page.locator('text=Valorización de Stock')).toBeVisible();
    await expect(page.locator('text=Alertas de Stock Mínimo')).toBeVisible();

    // Pestañas
    await expect(page.locator('[data-testid="tab-stock"]')).toBeVisible();
    await expect(page.locator('[data-testid="tab-kardex"]')).toBeVisible();
  });

  test('debe listar los insumos con sus existencias, unidad y estado', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page.locator('[data-testid="stock-table"]')).toBeVisible({ timeout: 10000 });

    // Verificar presencia de insumos demo
    await expect(page.locator('text=Carne de Res molida')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Pan de Hamburguesa')).toBeVisible();
    await expect(page.locator('text=Queso Cheddar')).toBeVisible();
  });

  test('debe alternar a la pestaña de Kardex y mostrar el historial de movimientos', async ({ page }) => {
    await page.goto('/inventory');

    const kardexTab = page.locator('[data-testid="tab-kardex"]');
    await kardexTab.click();

    await expect(page.locator('[data-testid="kardex-table"]')).toBeVisible({ timeout: 10000 });
    // Debe listar al menos movimientos de inventario inicial o ventas
    await expect(page.locator('text=Total movimientos registrados:')).toBeVisible();
  });

  test('debe abrir modal de ajuste físico y actualizar la cantidad', async ({ page }) => {
    await page.goto('/inventory');

    // Clic en el primer botón de ajuste de stock
    const adjustBtn = page.locator('button:has-text("⚙️ Ajustar Stock")').first();
    await expect(adjustBtn).toBeVisible({ timeout: 10000 });
    await adjustBtn.click();

    // Modal de ajuste visible
    await expect(page.locator('h2:has-text("Ajuste de Inventario Físico")')).toBeVisible();

    // Modificar cantidad física
    const qtyInput = page.locator('[data-testid="new-quantity-input"]');
    await qtyInput.fill('45.0000');

    // Confirmar ajuste
    await page.locator('[data-testid="confirm-adjust-btn"]').click();

    // Verificar alerta de éxito y modal cerrado
    await expect(page.locator('text=actualizado a 45')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('h2:has-text("Ajuste de Inventario Físico")')).not.toBeVisible();
  });
});
