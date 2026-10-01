import { test, expect } from '@playwright/test';

test.describe('Punto de Venta (POS) y Dexie.js Offline First', () => {
  test.beforeEach(async ({ page }) => {
    // Autenticar con Admin
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });
  });

  test('debe mostrar el estado de conectividad en línea y los productos disponibles', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Punto de Venta');
    await expect(page.locator('text=🟢 En Línea')).toBeVisible({ timeout: 10000 });

    // Verificar productos del menú cargados
    await expect(page.locator('text=Hamburguesa Clásica')).toBeVisible({ timeout: 10000 });
  });

  test('debe agregar productos a la comanda y calcular el total reactivamente', async ({ page }) => {
    // Esperar a que el producto esté disponible
    const addBurgerBtn = page.locator('[data-testid="add-btn-DISH-BURGER-01"]');
    await expect(addBurgerBtn).toBeVisible({ timeout: 10000 });
    await addBurgerBtn.click();

    // Verificar que aparece en el carrito de comanda
    const cartAside = page.locator('[data-testid="pos-cart"]');
    await expect(cartAside.locator('h4:has-text("Hamburguesa Clásica")')).toBeVisible();

    // El total debe ser calculado (Bs. 35.00)
    await expect(cartAside.locator('text=Total a Cobrar:')).toBeVisible();
    await expect(cartAside.locator('text=Bs. 35.00').first()).toBeVisible();
  });

  test('debe completar el flujo de cobro en efectivo y emitir ticket transaccional', async ({ page }) => {
    // Agregar producto al carrito
    const addBurgerBtn = page.locator('[data-testid="add-btn-DISH-BURGER-01"]');
    await expect(addBurgerBtn).toBeVisible({ timeout: 10000 });
    await addBurgerBtn.click();

    // Abrir modal de cobro desde el carrito
    const cartAside = page.locator('[data-testid="pos-cart"]');
    const cobrarBtn = cartAside.locator('button:has-text("Cobrar Pedido")');
    await cobrarBtn.click();

    // Modal de cobro visible
    await expect(page.locator('h2:has-text("Cobrar Comanda de Venta")')).toBeVisible();

    // Seleccionar monto exacto
    await page.locator('button:has-text("Exacto")').click();

    // Confirmar y emitir ticket
    const confirmBtn = page.locator('button:has-text("Confirmar y Emitir Ticket")');
    await confirmBtn.click();

    // Debe abrir el modal de comprobante con el ticket transaccional
    await expect(page.locator('#printable-ticket')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#printable-ticket')).toContainText('T-');
  });

  test('debe soportar venta en Modo Offline con persistencia en Dexie.js y sincronización', async ({ page }) => {
    // Asegurar que el catálogo ya está cargado en memoria/Dexie antes de desconectar
    const addCocaBtn = page.locator('[data-testid="add-btn-DISH-COCA-01"]');
    await expect(addCocaBtn).toBeVisible({ timeout: 10000 });

    // Simular corte de red
    await page.context().setOffline(true);

    // Debe indicar Modo Offline
    await expect(page.locator('text=🟠 Modo Offline')).toBeVisible({ timeout: 10000 });

    // Agregar producto en modo offline
    await addCocaBtn.click();

    // Cobrar comanda en offline
    const cartAside = page.locator('[data-testid="pos-cart"]');
    await cartAside.locator('button:has-text("Cobrar Pedido")').click();
    await page.locator('button:has-text("Exacto")').click();
    await page.locator('button:has-text("Confirmar y Emitir Ticket")').click();

    // Debe emitir ticket offline LOCAL-XXXX
    await expect(page.locator('#printable-ticket')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#printable-ticket')).toContainText('LOCAL-');

    // Cerrar modal de ticket con Nueva Venta
    await page.locator('button:has-text("Nueva Venta")').click();

    // Debe mostrar indicador de pedidos pendientes en Dexie.js
    await expect(page.locator('text=pendiente(s)')).toBeVisible({ timeout: 10000 });

    // Restaurar red
    await page.context().setOffline(false);
    await expect(page.locator('text=🟢 En Línea')).toBeVisible({ timeout: 10000 });

    // Sincronizar órdenes pendientes si el botón está visible
    const syncBtn = page.locator('button:has-text("Sincronizar Ahora")');
    if (await syncBtn.isVisible()) {
      await syncBtn.click();
    }

    // El contador de pendientes debe desaparecer tras sincronizar con el backend
    await expect(page.locator('text=pendiente(s)')).not.toBeVisible({ timeout: 15000 });
  });
});
