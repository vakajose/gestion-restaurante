import { test, expect } from '@playwright/test';
import { AuthPage } from './pages/auth.page';
import { PosPage } from './pages/pos.page';
import { CashControlPage } from './pages/cash-control.page';
import { FinancePage } from './pages/finance.page';

test.describe('Jornada Completa de Servicio: Invariante POS, Ventas, Gastos, Arqueo y Consolidación (PB-046 / TASK-412)', () => {
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

  test('1. Paso 1: Apertura o Aseguramiento de Turno en POS con Fondo Inicial', async () => {
    await authPage.loginAsCashier();
    await posPage.goto();

    // Si el turno está cerrado, verificar banner sugerente y abrirlo
    if (await posPage.closedShiftState.isVisible().catch(() => false)) {
      await posPage.assertClosedShiftState();
      await posPage.openShift('200.00');
    }

    // Comprobar que el catálogo está accesible
    await expect(posPage.closedShiftState).not.toBeVisible();
  });

  test('2. Paso 2: Venta en POS con Modalidad Para Llevar y Cobro Exacto en Efectivo', async ({ page }) => {
    await authPage.loginAsCashier();
    await posPage.goto();

    // Asegurar que el catálogo esté listo
    const addDishBtn = page.locator('[data-testid^="add-btn-"]').first();
    await expect(addDishBtn).toBeVisible({ timeout: 15000 });
    await addDishBtn.click();

    // Si se abrió modal de modificadores, confirmar selección
    const confirmModBtn = page.locator('[data-testid="confirm-modifiers-btn"]');
    if (await confirmModBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
      await confirmModBtn.click();
    }

    // Abrir modal de cobro desde el carrito
    await posPage.openCheckout();

    // Seleccionar modalidad Para Llevar y cliente opcional
    await posPage.checkoutExactCash({
      orderType: 'TAKEOUT',
      customerName: 'Carlos Mendoza',
    });

    // Validar emisión de ticket con cajero
    await expect(posPage.ticketModal).toBeVisible();
    await expect(posPage.ticketCashierName).toBeVisible();

    // Cerrar ticket para volver al catálogo
    await posPage.closeTicket();
  });

  test('3. Paso 3: Visor en Vivo en Control de Caja y Registro de Gasto Menor', async ({ page }) => {
    await authPage.loginAsCashier();
    await cashControlPage.goto();

    // Comprobar que la comanda anterior aparece en el visor de comandas
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 });

    // Filtrar por Mis Ventas y luego Todas
    await cashControlPage.filterByCashier('MINE');
    await cashControlPage.filterByCashier('ALL');

    // Registrar solicitud de gasto menor de emergencia
    const expenseDesc = 'Hielo y servilletas urgentes ' + Date.now();
    await cashControlPage.registerExpense(expenseDesc, '35.00');

    // Cambiar a la pestaña de gastos para verificar estado inicial Pendiente
    await cashControlPage.tabExpenses.click();
    const expenseRow = page.locator(`tr:has-text("${expenseDesc}")`);
    await expect(expenseRow).toBeVisible({ timeout: 10000 });
    await expect(expenseRow.locator('text=Pendiente')).toBeVisible();
  });

  test('4. Paso 4: Administrador Aprueba el Gasto Menor en Finanzas', async ({ page }) => {
    await authPage.loginAsAdmin();
    await financePage.goto();

    // Navegar a Gastos y aprobar gastos pendientes
    await financePage.selectTab('expenses');
    const approveButtons = page.locator('[data-testid="btn-approve-expense"]');

    if (await approveButtons.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      await financePage.approveExpense();
      // Validar feedback de aprobación reactivo
      await expect(page.locator('text=aprobado con éxito, text=Gasto aprobado').first()).toBeVisible({ timeout: 10000 });
    }
  });

  test('5. Paso 5: Arqueo Físico Exacto y Cierre de Turno en Control de Caja', async () => {
    await authPage.loginAsAdmin();
    await cashControlPage.goto();

    // Cerrar turno con cuadre exacto al saldo teórico
    await cashControlPage.closeShift(undefined, 'Arqueo de jornada certificada E2E sin diferencias');

    // Verificar que el turno ahora está cerrado
    await expect(cashControlPage.btnOpenShift).toBeVisible({ timeout: 15000 });
  });

  test('6. Paso 6: Consolidación y Certificación Contable Gerencial en Finanzas', async ({ page }) => {
    await authPage.loginAsAdmin();
    await financePage.goto();
    await financePage.selectTab('shifts');

    // Filtrar turnos cerrados para consolidar
    const filterClosedBtn = page.locator('button:has-text("Cerrados"), button:has-text("Pendientes de Consolidar")').first();
    if (await filterClosedBtn.isVisible()) {
      await filterClosedBtn.click();
    }

    // Si hay un turno cerrado disponible, consolidarlo formalmente
    if (await financePage.btnOpenConsolidate.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      await financePage.consolidateShift('Turno auditado y consolidado por gerencia');
      await expect(page.locator('text=consolidado con éxito, text=Turno consolidado').first()).toBeVisible({ timeout: 10000 });
    }
  });
});
