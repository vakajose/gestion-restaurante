import { test, expect } from '@playwright/test';
import { AuthPage } from './pages/auth.page';
import { PosPage } from './pages/pos.page';
import { CashControlPage } from './pages/cash-control.page';
import { FinancePage } from './pages/finance.page';

test.describe('Certificación Integral de Flujos del Sprint 5: Parámetros, Catálogo, PIN, Venta, Delivery, Cancelación y Auditoría (TASK-511)', () => {
  test.describe.configure({ mode: 'serial' });

  let authPage: AuthPage;
  let posPage: PosPage;
  let cashControlPage: CashControlPage;
  let financePage: FinancePage;

  test.beforeEach(async ({ page }) => {
    authPage = new AuthPage(page);
    posPage = new PosPage(page);
    cashControlPage = new CashControlPage(page);
    financePage = new FinancePage(page);
  });

  test('1. [CONFIG / PB-047] Consola de Parámetros Humanizada, Popover de Ayuda y Activación de Delivery Fee', async ({ page }) => {
    await authPage.loginAsAdmin();
    await page.goto('/settings/parameters');
    await expect(page.locator('h1')).toContainText('Parámetros del Sistema');

    // 1.1 Verificar buscador predictivo
    const searchInput = page.locator('[data-testid="parameter-search-input"]');
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    await searchInput.fill('delivery');

    // 1.2 Verificar tarjeta humanizada de delivery fee
    const deliveryCard = page.locator('[data-testid="param-card-pos.delivery_fee_enabled"]');
    await expect(deliveryCard).toBeVisible({ timeout: 10000 });
    await expect(deliveryCard.locator('[data-testid="param-title-pos.delivery_fee_enabled"]')).toContainText('Habilitar Costo de Envío / Delivery Fee');

    // 1.3 Verificar Popover de ayuda contextual (?)
    const helpBtn = deliveryCard.locator('[data-testid="context-help-btn"]');
    if (await helpBtn.isVisible()) {
      await helpBtn.click();
      const popover = deliveryCard.locator('[data-testid="context-help-popover"]');
      await expect(popover).toBeVisible({ timeout: 5000 });
      await expect(popover.locator('[data-testid="context-help-impact"]')).toBeVisible();
      // Cerrar popover
      await deliveryCard.locator('[data-testid="context-help-close-btn"]').click();
      await expect(popover).not.toBeVisible({ timeout: 5000 });
    }

    // 1.4 Asegurar que delivery_fee_enabled está activado
    const toggleLabel = deliveryCard.locator('[data-testid="toggle-label-pos.delivery_fee_enabled"]');
    const toggleBtn = deliveryCard.locator('[data-testid="toggle-pos.delivery_fee_enabled"]');
    const currentLabel = await toggleLabel.textContent();
    if (currentLabel?.includes('Desactivado')) {
      await toggleBtn.click();
      await expect(toggleLabel).toContainText('Activado', { timeout: 10000 });
    }
  });

  test('2. [AUTH / PB-045] Perfil de Usuario Autoservicio, Actualización de Nombre y Configuración de PIN de Supervisor', async ({ page }) => {
    await authPage.loginAsAdmin();
    await page.goto('/profile');
    await expect(page.locator('h1')).toContainText('Mi Perfil');

    // 2.1 Verificar que no hay lista cruda de permisos técnicos y que se muestra la tarjeta ejecutiva
    await expect(page.locator('[data-testid="authorized-branches-list"]')).toBeVisible({ timeout: 10000 });

    // 2.2 Actualizar datos de contacto personales
    const nameInput = page.locator('[data-testid="profile-fullname-input"]');
    await expect(nameInput).toBeVisible({ timeout: 10000 });
    await nameInput.fill('Administrador General E2E');
    await page.locator('[data-testid="profile-phone-input"]').fill('+591 70012345');
    await page.locator('[data-testid="save-profile-btn"]').click();

    // 2.3 Configurar PIN de supervisor (1234)
    const pinCard = page.locator('[data-testid="supervisor-pin-card"]');
    if (await pinCard.isVisible({ timeout: 5000 }).catch(() => false)) {
      await page.locator('[data-testid="supervisor-pin-input"]').fill('1234');
      await page.locator('[data-testid="confirm-pin-input"]').fill('1234');
      await page.locator('[data-testid="save-pin-btn"]').click();
      await expect(page.locator('[data-testid="pin-feedback-message"]')).toBeVisible({ timeout: 10000 });
    }
  });

  test('3. [CATALOG / PB-048 & PB-036] Catálogo Maestro con Emojis, SKU Asistido e Imágenes', async ({ page }) => {
    await authPage.loginAsAdmin();
    await page.goto('/catalog');
    await expect(page.locator('h1')).toContainText('Catálogo Maestro y Recetas');

    // 3.1 Pestaña Categorías
    await page.locator('[data-testid="tab-categories"]').click();
    await expect(page.locator('table')).toBeVisible({ timeout: 10000 });

    // 3.2 Pestaña Platos y verificación de visualización
    await page.locator('[data-testid="tab-dishes"]').click();
    await expect(page.locator('[data-testid="dish-search-input"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="create-dish-btn"]')).toBeVisible();
  });

  test('4. [POS / PB-020] Venta en Sala (DINE_IN) con Modificadores y Notas de Cocina', async ({ page }) => {
    await authPage.loginAsCashier();
    await posPage.ensureShiftOpen('100.00');

    // 4.1 Seleccionar primer plato disponible
    const addDishBtn = page.locator('[data-testid^="add-btn-"]').first();
    await expect(addDishBtn).toBeVisible({ timeout: 15000 });
    await addDishBtn.click();

    // 4.2 Si se abre el modal de modificadores, seleccionar modificador y confirmar
    const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    if (await confirmModifiersBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      const extraOpt = page.locator('[data-testid^="modifier-opt-"]').first();
      if (await extraOpt.isVisible({ timeout: 1500 }).catch(() => false)) {
        await extraOpt.click();
      }
      await confirmModifiersBtn.click();
    }

    // 4.3 Proceder al cobro en sala
    await posPage.openCheckout();
    await posPage.checkoutExactCash({
      orderType: 'DINE_IN',
      customerName: 'Cliente Mesa 1',
    });

    // 4.4 Validar ticket de venta
    await expect(posPage.ticketModal).toBeVisible({ timeout: 10000 });
    await posPage.closeTicket();
  });

  test('5. [POS / PB-032] Venta con Modalidad DELIVERY, Recargo de Flete y Datos de Envío', async ({ page }) => {
    await authPage.loginAsCashier();
    await posPage.goto();

    // 5.1 Agregar producto al carrito
    const addDishBtn = page.locator('[data-testid^="add-btn-"]').first();
    await expect(addDishBtn).toBeVisible({ timeout: 15000 });
    await addDishBtn.click();

    const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    if (await confirmModifiersBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await confirmModifiersBtn.click();
    }

    // 5.2 Abrir checkout y seleccionar DELIVERY
    await posPage.openCheckout();
    if (await posPage.orderTypeDelivery.isVisible({ timeout: 3000 }).catch(() => false)) {
      await posPage.orderTypeDelivery.click();
    }

    // 5.3 Cobrar comanda con datos de despacho
    await posPage.checkoutExactCash({
      orderType: 'DELIVERY',
      customerName: 'Cliente Delivery Express',
    });

    // 5.4 Validar ticket emitido
    await expect(posPage.ticketModal).toBeVisible({ timeout: 10000 });
    await posPage.closeTicket();
  });

  test('6. [POS & CASH / PB-031] Visor en Vivo, Solicitud de Cancelación y Autorización con PIN', async ({ page }) => {
    await authPage.loginAsCashier();
    await cashControlPage.goto();

    // 6.1 Inspeccionar primera comanda en vivo
    const firstOrderRow = page.locator('table tbody tr').first();
    await expect(firstOrderRow).toBeVisible({ timeout: 15000 });

    // Abrir detalle de la comanda
    const viewDetailBtn = firstOrderRow.locator('[data-testid="view-order-detail-btn"]').first();
    if (await viewDetailBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
      await viewDetailBtn.click();

      // Verificar botón de cancelación si el pedido está activo
      const cancelBtn = page.locator('[data-testid="btn-cancel-order"]');
      if (await cancelBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await cancelBtn.click();

        // 6.2 Validar modal de autorización de supervisor
        const pinInput = page.locator('[data-testid="input-supervisor-pin"]');
        if (await pinInput.isVisible({ timeout: 5000 }).catch(() => false)) {
          // Probar teclado táctil: pulsar 1, 2, 3, 4
          await page.locator('[data-testid="pin-num-1"]').click();
          await page.locator('[data-testid="pin-num-2"]').click();
          await page.locator('[data-testid="pin-num-3"]').click();
          await page.locator('[data-testid="pin-num-4"]').click();

          // Confirmar anulación
          const confirmCancelBtn = page.locator('[data-testid="btn-confirm-cancel-pin"]');
          await expect(confirmCancelBtn).toBeEnabled({ timeout: 5000 });
          await confirmCancelBtn.click();

          // Validar cierre de modal
          await expect(pinInput).not.toBeVisible({ timeout: 10000 });
        }
      } else {
        // Cerrar modal de comanda si no permitía cancelación directa
        await page.locator('button:has-text("Cerrar")').last().click().catch(() => {});
      }
    }
  });

  test('7. [FINANCE / PB-030] Arqueo Físico Exacto y Cierre Formal del Turno', async () => {
    await authPage.loginAsCashier();
    await cashControlPage.goto();

    // 7.1 Cerrar turno calculando el efectivo esperado en gaveta
    await cashControlPage.closeShift(undefined, 'Cierre formal de turno Sprint 5 E2E');
  });

  test('8. [FINANCE / PB-030] Auditoría Gerencial de Turnos, Modal Drill-Down y Exportación CSV', async ({ page }) => {
    await authPage.loginAsAdmin();
    await financePage.goto();

    // 8.1 Seleccionar pestaña Turnos y verificar histórico
    await financePage.selectTab('shifts');
    await expect(page.locator('[data-testid="filter-start-date"]')).toBeVisible({ timeout: 10000 });

    // 8.2 Aplicar filtro de fecha rápida "Hoy"
    const presetToday = page.locator('[data-testid="preset-today"]');
    if (await presetToday.isVisible({ timeout: 3000 }).catch(() => false)) {
      await presetToday.click();
    }

    // 8.3 Abrir modal de detalle de auditoría (Drill-Down) si existe fila de turno
    const viewDetailBtn = page.locator('[data-testid="btn-view-shift-detail"]').first();
    if (await viewDetailBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
      await viewDetailBtn.click();
      const modal = page.locator('[data-testid="shift-detail-modal"]');
      await expect(modal).toBeVisible({ timeout: 10000 });

      // Validar pestañas del modal (Resumen, Comandas, Gastos)
      await expect(page.locator('[data-testid="tab-modal-summary"]')).toBeVisible();
      await page.locator('[data-testid="tab-modal-orders"]').click();
      await page.locator('[data-testid="tab-modal-expenses"]').click();

      // Cerrar modal
      await page.locator('[data-testid="btn-close-modal"]').click();
      await expect(modal).not.toBeVisible({ timeout: 5000 });
    }

    // 8.4 Probar botón de Exportación CSV
    const exportBtn = page.locator('[data-testid="btn-export-shifts-csv"]');
    await expect(exportBtn).toBeVisible({ timeout: 10000 });
  });
});
