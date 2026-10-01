import { test, expect } from '@playwright/test';

test.describe('Autenticación y Tema (TailwindAdmin)', () => {
  test('debe cargar la vista de login con los estilos y botones de acceso rápido', async ({ page }) => {
    await page.goto('/auth/login');

    // Verificar branding y formulario
    await expect(page.locator('h1')).toContainText('Gestión Restaurante');
    await expect(page.locator('#login-input')).toBeVisible();
    await expect(page.locator('#password-input')).toBeVisible();
    await expect(page.locator('[data-testid="demo-admin-btn"]')).toBeVisible();
    await expect(page.locator('[data-testid="demo-cashier-btn"]')).toBeVisible();
  });

  test('debe alternar entre Modo Claro y Modo Oscuro con ThemeService', async ({ page }) => {
    await page.goto('/auth/login');

    const toggleBtn = page.locator('[data-testid="theme-toggle-btn"]');
    await expect(toggleBtn).toBeVisible();

    // Obtener estado inicial
    const initialText = await toggleBtn.textContent();

    // Clic para cambiar tema
    await toggleBtn.click();
    await expect(toggleBtn).not.toHaveText(initialText || '');

    // Verificar clase 'dark' en el elemento html
    const htmlElement = page.locator('html');
    if (initialText?.includes('Modo Oscuro')) {
      await expect(htmlElement).toHaveClass(/dark/);
    } else {
      await expect(htmlElement).not.toHaveClass(/dark/);
    }
  });

  test('debe autenticar al usuario Admin usando el botón Demo Admin y redirigir al POS', async ({ page }) => {
    await page.goto('/auth/login');

    // Clic en atajo Demo Admin
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await expect(page.locator('#login-input')).toHaveValue('admin');
    await expect(page.locator('#password-input')).toHaveValue('admin123');

    // Enviar formulario
    await page.locator('[data-testid="login-submit-btn"]').click();

    // Debe redirigir automáticamente a /pos
    await page.waitForURL('**/pos', { timeout: 15000 });
    await expect(page).toHaveURL(/.*\/pos/);

    // Verificar que el token JWT se guardó en localStorage
    const authToken = await page.evaluate(() => localStorage.getItem('auth_token'));
    expect(authToken).toBeTruthy();
  });
});
