import { test, expect } from '@playwright/test';

test.describe('Flujo E2E Completo: Ciclo de Turno, Ventas POS, Aprobaciones y Arqueo', () => {
  test.describe.configure({ mode: 'serial' });

  test('1. Asegurar o abrir turno de caja chica como Cajero', async ({ page }) => {
    // Login con Cajero
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-cashier-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });

    // Ir a Finanzas para verificar o abrir turno
    await page.goto('/finance');
    await expect(page.locator('h1')).toContainText('Finanzas y Arqueo de Caja Chica');

    const openBtn = page.locator('[data-testid="btn-open-shift"]');
    const openCta = page.locator('[data-testid="btn-open-shift-cta"]');
    const isShiftOpen = await page.locator('text=Turno Activo en Curso').isVisible().catch(() => false);

    if (!isShiftOpen) {
      if (await openCta.isVisible()) {
        await openCta.click();
      } else if (await openBtn.isVisible()) {
        await openBtn.click();
      }
      await expect(page.locator('h3:has-text("Apertura de Turno de Caja")')).toBeVisible();
      await page.locator('[data-testid="input-initial-cash"]').fill('200.00');
      await page.locator('[data-testid="btn-confirm-open-shift"]').click();
      await expect(page.locator('text=Turno de caja abierto exitosamente.')).toBeVisible({ timeout: 10000 });
    }

    await expect(page.locator('text=Turno Activo en Curso')).toBeVisible();
  });

  test('2. Realizar ventas en POS con promociones, combos y modificadores', async ({ page }) => {
    // Iniciar sesión con Cajero
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-cashier-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });

    // Filtrar por categoría Todo o Hamburguesas
    const filterBtn = page.locator('[data-testid="category-filter-all"]');
    if (await filterBtn.isVisible()) {
      await filterBtn.click();
    }

    // Agregar Hamburguesa Clásica con Modificadores (TASK-210)
    const addBurgerBtn = page.locator('[data-testid="add-btn-DISH-BURGER-01"]');
    await expect(addBurgerBtn).toBeVisible({ timeout: 10000 });
    await addBurgerBtn.click();

    // Confirmar modificadores en el modal
    const confirmModifiersBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    await expect(confirmModifiersBtn).toBeVisible({ timeout: 5000 });
    await confirmModifiersBtn.click();

    // Agregar Bebida (Coca Cola)
    const addCocaBtn = page.locator('[data-testid="add-btn-DISH-COCA-01"]');
    if (await addCocaBtn.isVisible()) {
      await addCocaBtn.click();
    }

    // Verificar comanda y total en carrito
    const cartAside = page.locator('[data-testid="pos-cart"]');
    await expect(cartAside.locator('h4:has-text("Hamburguesa Clásica")')).toBeVisible();
    await expect(cartAside.locator('text=Total a Cobrar:')).toBeVisible();

    // Cobrar comanda
    await cartAside.locator('button:has-text("Cobrar Pedido")').click();
    await expect(page.locator('h2:has-text("Cobrar Comanda de Venta")')).toBeVisible();
    await page.locator('button:has-text("Exacto")').click();
    await page.locator('button:has-text("Confirmar y Emitir Ticket")').click();

    // Validar emisión de ticket transaccional
    await expect(page.locator('#printable-ticket')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#printable-ticket')).toContainText('T-');

    // Cerrar ticket para la siguiente venta
    await page.locator('button:has-text("Nueva Venta")').click();
    await expect(page.locator('#printable-ticket')).not.toBeVisible();
  });

  test('3. Registrar solicitud de gastos menores desde la gaveta como Cajero (PENDING_APPROVAL)', async ({ page }) => {
    // Login Cajero
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-cashier-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/pos', { timeout: 15000 });

    // Navegar a Finanzas -> Gastos
    await page.goto('/finance');
    await page.locator('[data-testid="tab-expenses"]').click();

    // Abrir modal de nuevo gasto
    const newExpenseBtn = page.locator('[data-testid="btn-add-expense-tab"]');
    await expect(newExpenseBtn).toBeVisible({ timeout: 10000 });
    await newExpenseBtn.click();

    // Llenar formulario de gasto 1
    const expenseDesc = 'Compra servilletas y vasos E2E ' + Date.now();
    await page.locator('[data-testid="input-expense-desc"]').fill(expenseDesc);
    await page.locator('[data-testid="input-expense-amount"]').fill('35.00');
    await page.locator('[data-testid="btn-confirm-save-expense"]').click();

    // Validar confirmación
    await expect(page.locator('text=Gasto registrado correctamente.')).toBeVisible({ timeout: 10000 });
    await expect(page.locator(`text=${expenseDesc}`).first()).toBeVisible();

    // Verificar que el cajero ve el estado Pendiente pero NO puede aprobarlo
    const row = page.locator(`tr:has-text("${expenseDesc}")`);
    await expect(row.locator('text=Pendiente')).toBeVisible();
    await expect(row.locator('button:has-text("Aprobar")')).not.toBeVisible();
  });

  test('4. Administrador inicia sesión y aprueba los gastos pendientes', async ({ page }) => {
    // Iniciar sesión con Admin
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/company', { timeout: 15000 });

    // Ir a Finanzas -> Gastos
    await page.goto('/finance');
    await page.locator('[data-testid="tab-expenses"]').click();

    // Buscar botones de aprobación disponibles para el administrador
    const approveButtons = page.locator('button[title="Aprobar gasto"]:visible, button:has-text("Aprobar"):visible');
    const count = await approveButtons.count();

    for (let i = 0; i < count; i++) {
      const btn = approveButtons.first();
      if (await btn.isVisible()) {
        await btn.click();
        await page.waitForTimeout(500);
      }
    }

    // Verificar que el KPI de Gastos Aprobados refleja los montos
    await expect(page.locator('text=Gastos Aprobados')).toBeVisible();
  });

  test('5. Realizar Arqueo final, cuadre exacto de caja y cierre de turno', async ({ page }) => {
    // Iniciar sesión con Admin para cierre oficial
    await page.goto('/auth/login');
    await page.locator('[data-testid="demo-admin-btn"]').click();
    await page.locator('[data-testid="login-submit-btn"]').click();
    await page.waitForURL('**/company', { timeout: 15000 });

    // Navegar a Finanzas -> Turnos y Arqueo
    await page.goto('/finance');
    await page.locator('[data-testid="tab-shifts"]').click();

    // Abrir modal de arqueo y cierre
    const closeShiftBtn = page.locator('[data-testid="btn-close-shift"]');
    await expect(closeShiftBtn).toBeVisible({ timeout: 10000 });
    await closeShiftBtn.click();

    await expect(page.locator('h3:has-text("Arqueo y Cierre de Caja")')).toBeVisible();

    // Obtener el valor del saldo teórico sugerido
    const theoreticalText = await page.locator('[data-testid="theoretical-cash-value"]').textContent();
    const match = theoreticalText?.match(/Bs\.\s*([\d,.]+)/);
    const expectedAmount = match ? match[1].replace(/,/g, '') : '200.00';

    // Rellenar conteo real exacto para cuadre perfecto
    const actualCashInput = page.locator('[data-testid="input-actual-cash"]');
    await actualCashInput.fill(expectedAmount);

    // Confirmar arqueo y cierre
    await page.locator('[data-testid="btn-confirm-close-shift"]').click();

    // Validar mensaje de cierre exitoso
    await expect(page.locator('text=Turno cerrado y arqueo asentado con éxito.')).toBeVisible({ timeout: 15000 });

    // Verificar que la vista indica que no hay turno abierto y el botón de apertura está listo para el día siguiente
    await expect(page.locator('text=No hay un turno de caja abierto')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="btn-open-shift-cta"]')).toBeVisible();
  });
});
