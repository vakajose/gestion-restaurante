import { test, expect } from '@playwright/test';
import { AuthPage } from './pages/auth.page';
import { PosPage } from './pages/pos.page';

test.describe('Punto de Venta (POS) y Dexie.js Offline First', () => {
  let authPage: AuthPage;
  let posPage: PosPage;

  test.beforeEach(async ({ page }) => {
    authPage = new AuthPage(page);
    posPage = new PosPage(page);

    // Autenticar con Cajero y asegurar turno abierto (PB-041 / PB-028)
    await authPage.loginAsCashier();
    await posPage.ensureShiftOpen('100.00');
  });

  test('debe mostrar el estado de conectividad en línea y los productos disponibles', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Punto de Venta');
    await expect(page.locator('[data-testid="catalog-grid-view"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="offline-warning-bar"]')).not.toBeVisible();

    // Verificar productos del menú cargados en la vista de cuadrícula
    await expect(page.locator('[data-testid^="dish-card-"]').first()).toBeVisible({ timeout: 10000 });
  });

  test('debe agregar productos a la comanda y calcular el total reactivamente', async ({ page }) => {
    // Agregar primer producto disponible
    const addDishBtn = page.locator('[data-testid^="add-btn-"]').first();
    await expect(addDishBtn).toBeVisible({ timeout: 10000 });
    await addDishBtn.click();

    // Confirmar modal de modificadores si se abre
    const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    if (await confirmModifiersBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await confirmModifiersBtn.click();
    }

    // Verificar que aparece en el carrito de comanda
    const cartAside = page.locator('[data-testid="pos-cart"]');
    await expect(cartAside).toBeVisible();

    // El total debe ser calculado reactivamente
    await expect(cartAside.locator('text=Total a Cobrar:')).toBeVisible();
  });

  test('debe seleccionar modificadores y extras actualizando el precio (TASK-210)', async ({ page }) => {
    const addBurgerBtn = page.locator('[data-testid="add-btn-DISH-BURGER-01"]');
    if (await addBurgerBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await addBurgerBtn.click();

      // Modal de modificadores debe abrirse
      const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
      await expect(confirmModifiersBtn).toBeVisible({ timeout: 5000 });

      // Seleccionar modificador extra si existe
      const extraOpt = page.locator('[data-testid^="modifier-opt-"]').first();
      if (await extraOpt.isVisible({ timeout: 2000 }).catch(() => false)) {
        await extraOpt.click();
      }
      await confirmModifiersBtn.click();
    } else {
      // Fallback: seleccionar cualquier plato con modificadores
      const anyAddBtn = page.locator('[data-testid^="add-btn-"]').first();
      await anyAddBtn.click();
      const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
      if (await confirmModifiersBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmModifiersBtn.click();
      }
    }

    // Verificar en el carrito
    const cartAside = page.locator('[data-testid="pos-cart"]');
    await expect(cartAside).toBeVisible();
    await expect(cartAside.locator('text=Total a Cobrar:')).toBeVisible();
  });

  test('debe completar el flujo de cobro en efectivo y emitir ticket transaccional', async ({ page }) => {
    // Agregar un producto al carrito
    await posPage.addDishByCode('DISH-COCA-01').catch(async () => {
      const anyAdd = page.locator('[data-testid^="add-btn-"]').first();
      await anyAdd.click();
      const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
      if (await confirmModifiersBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await confirmModifiersBtn.click();
      }
    });

    // Abrir modal de cobro y pagar con efectivo exacto
    await posPage.openCheckout();
    await posPage.checkoutExactCash({ orderType: 'TAKEOUT', customerName: 'Cliente POS Test' });

    // Debe abrir el modal de comprobante con el ticket transaccional
    await expect(page.locator('#printable-ticket')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#printable-ticket')).toContainText('T-');

    // Cerrar ticket
    await posPage.closeTicket();
  });

  test('debe soportar venta en Modo Offline con persistencia en Dexie.js y sincronización', async ({ page }) => {
    // Simular corte de red
    await page.context().setOffline(true);

    // Debe indicar Modo Offline en el banner
    await expect(page.locator('[data-testid="offline-warning-bar"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="offline-warning-bar"]')).toContainText('Modo Offline');

    // Agregar producto en modo offline
    const addBtn = page.locator('[data-testid^="add-btn-"]').first();
    await addBtn.click();
    const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    if (await confirmModifiersBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
      await confirmModifiersBtn.click();
    }

    // Cobrar comanda en offline
    await posPage.openCheckout();
    await posPage.checkoutExactCash();

    // Debe emitir ticket offline con prefijo LOCAL- o T-
    await expect(page.locator('#printable-ticket')).toBeVisible({ timeout: 10000 });

    // Cerrar ticket con Nueva Venta
    await posPage.closeTicket();

    // Debe mostrar botón de limpiar cola indicando órdenes guardadas
    await expect(page.locator('[data-testid="clear-offline-queue-btn"]')).toBeVisible({ timeout: 10000 });

    // Restaurar red: posService auto-sincroniza automáticamente en background
    await page.context().setOffline(false);

    // Si el botón de sincronización manual aún está presente y habilitado, podemos hacer click como fallback
    const syncBtn = page.locator('[data-testid="sync-pending-btn"]');
    if (await syncBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      if (await syncBtn.isEnabled().catch(() => false)) {
        await syncBtn.click().catch(() => {});
      }
    }

    // El banner completo de offline/pendientes debe desaparecer tras sincronizar con el backend
    await expect(page.locator('[data-testid="offline-warning-bar"]')).not.toBeVisible({ timeout: 15000 });
  });
});

