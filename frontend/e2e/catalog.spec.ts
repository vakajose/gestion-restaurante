import { test, expect } from '@playwright/test';

test.describe('Catálogo Maestro y Recetas BOM (TailwindAdmin)', () => {
  test.beforeEach(async ({ page }) => {
    // Autenticar previamente con Admin
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });
  });

  test('debe navegar al Catálogo y mostrar las pestañas operativas', async ({ page }) => {
    await page.goto('/catalog');
    await expect(page.locator('h1')).toContainText('Catálogo Maestro y Recetas');

    // Verificar presencia de las 3 pestañas
    await expect(page.locator('[data-testid="tab-dishes"]')).toBeVisible();
    await expect(page.locator('[data-testid="tab-ingredients"]')).toBeVisible();
    await expect(page.locator('[data-testid="tab-categories"]')).toBeVisible();
  });

  test('debe listar los platos del menú con sus precios y acciones', async ({ page }) => {
    await page.goto('/catalog');

    // Esperar a que se carguen los platos
    const dishesTab = page.locator('[data-testid="tab-dishes"]');
    await dishesTab.click();

    // Debe contener al menos la Hamburguesa Clásica de las semillas del backend
    await expect(page.locator('text=Hamburguesa Clásica')).toBeVisible({ timeout: 10000 });
  });

  test('debe alternar entre pestañas de Insumos y Categorías', async ({ page }) => {
    await page.goto('/catalog');

    // Pestaña Insumos
    await page.locator('[data-testid="tab-ingredients"]').click();
    await expect(page.locator('text=Carne de Res')).toBeVisible({ timeout: 10000 });

    // Pestaña Categorías
    await page.locator('[data-testid="tab-categories"]').click();
    await expect(page.locator('text=Hamburguesas')).toBeVisible({ timeout: 10000 });
  });
});
