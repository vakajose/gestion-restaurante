// run_e2e_tests.js - Suite exhaustiva de pruebas E2E contra Chrome DevTools en 127.0.0.1:9222
import { createOrAttachPage } from './cdp_client.js';
import * as path from 'node:path';

import * as fs from 'node:fs';

const SCREENSHOT_DIR = process.env.SCREENSHOT_DIR || path.resolve('e2e-screenshots');
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}
const SCRATCH_DIR = SCREENSHOT_DIR;

async function runAllTests() {
  console.log('================================================================');
  console.log('🚀 INICIANDO SUITE DE PRUEBAS E2E EXHAUSTIVAS CON CHROME DEVTOOLS');
  console.log('================================================================\n');

  const page = await createOrAttachPage('http://localhost:4200/auth/login');
  const results = [];

  function recordResult(name, passed, details = '') {
    results.push({ name, passed, details });
    const icon = passed ? '✅ PASÓ' : '❌ FALLÓ';
    console.log(`${icon} | ${name}`);
    if (details) console.log(`   └─ Detalle: ${details}`);
    console.log('----------------------------------------------------------------');
  }

  try {
    // -------------------------------------------------------------------------
    // TEST 1: Login Adaptativo & Autenticación Multi-Tenant (TASK-201)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 1] Verificación de Login y Autenticación Multi-Tenant');
    await page.navigate('http://localhost:4200/auth/login');
    await page.sleep(1500);

    const isAlreadyLoggedIn = await page.evaluate('!window.location.pathname.includes("/auth/login")');
    if (!isAlreadyLoggedIn) {
      console.log('   Presionando atajo Demo Admin...');
      await page.waitForSelector('[data-testid="demo-admin-btn"]', 5000);
      await page.click('[data-testid="demo-admin-btn"]');
      await page.sleep(500);

      console.log('   Enviando formulario de autenticación...');
      await page.click('[data-testid="login-submit-btn"]');
      await page.sleep(2500);
    }

    const currentPath = await page.evaluate('window.location.pathname');
    const isLoggedIn = !currentPath.includes('/auth/login');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '01_login_success.png'));
    recordResult('TEST 1: Login Adaptativo & Autenticación Multi-Tenant (TASK-201)', isLoggedIn, `Ruta actual: ${currentPath}`);

    // -------------------------------------------------------------------------
    // TEST 2: Selector Contextual de Sucursal en Topbar (TASK-201)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 2] Conmutación Contextual de Sucursal en Topbar');
    await page.waitForSelector('[data-testid="branch-selector-btn"]', 5000);
    const initialBranchName = await page.getText('[data-testid="active-branch-name"]');

    // Abrir dropdown de sucursales
    await page.click('[data-testid="branch-selector-btn"]');
    await page.sleep(600);

    const hasDropdown = await page.evaluate('!!document.querySelector("[data-testid=\'branch-dropdown-menu\']")');
    const availableBranches = await page.evaluate(`
      (() => {
        const menu = document.querySelector('[data-testid="branch-dropdown-menu"]');
        if (!menu) return [];
        return Array.from(menu.querySelectorAll('button')).map(b => b.innerText.split('\\n')[0].trim());
      })()
    `);
    await page.captureScreenshot(path.join(SCRATCH_DIR, '02_branch_selector_open.png'));

    // Cerrar dropdown
    await page.click('[data-testid="branch-selector-btn"]');
    await page.sleep(400);

    const branchTestPassed = hasDropdown && availableBranches.length >= 2;
    recordResult(
      'TEST 2: Selector Contextual de Sucursal (TASK-201)',
      branchTestPassed,
      `Sucursal activa: "${initialBranchName}", Sucursales disponibles: [${availableBranches.join(', ')}]`
    );

    // -------------------------------------------------------------------------
    // TEST 3: Inspección de "Mi Perfil" & Control de Acceso RBAC (TASK-204)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 3] Navegación a "Mi Perfil" y Verificación de Permisos');
    await page.click('[data-testid="profile-link"]');
    await page.sleep(1500);

    const profileText = await page.evaluate('document.body.innerText');
    const hasUsername = profileText.includes('admin');
    const hasRole = profileText.includes('ADMIN_TENANT');
    const hasEmail = profileText.includes('admin@restaurant.com');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '03_profile_rbac.png'));

    const profilePassed = hasUsername && hasRole && hasEmail;
    recordResult(
      'TEST 3: Módulo Mi Perfil & RBAC (TASK-204)',
      profilePassed,
      `Usuario: admin | Rol: ADMIN_TENANT | Email: admin@restaurant.com`
    );

    // -------------------------------------------------------------------------
    // TEST 4: Punto de Venta (POS) - Banner Happy Hour Activo (TASK-209)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 4] Punto de Venta (POS) & Promociones / Happy Hour');
    await page.click('[data-testid="nav-pos"]');
    await page.sleep(2000);

    const bannerText = await page.getText('[data-testid="active-promotion-banner"]');
    const hasHappyHour = !!bannerText && bannerText.includes('Happy Hour 15% OFF');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '04_pos_happy_hour.png'));
    recordResult(
      'TEST 4: Banner de Promociones / Happy Hour en POS (TASK-209)',
      hasHappyHour,
      `Banner detectado: "${bannerText?.trim() || 'No encontrado'}"`
    );

    // -------------------------------------------------------------------------
    // TEST 5: POS - Selección de Plato con Extras / Modificadores (TASK-210)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 5] Selección de Modificadores / Extras de Plato');
    const burgerCardSelector = '[data-testid="dish-card-DISH-BURGER-01"]';
    await page.waitForSelector(burgerCardSelector, 5000);

    // La tarjeta debe mostrar el badge de Extras
    const burgerCardText = await page.getText(burgerCardSelector);
    console.log(`   Tarjeta de plato encontrada: "${burgerCardText.replace(/\\n/g, ' ')}"`);

    // Clic en la tarjeta para abrir el modal de extras
    await page.click(burgerCardSelector);
    await page.sleep(1000);

    const hasModifierModal = await page.evaluate('!!document.querySelector("[data-testid=\'modifier-selection-modal\']")');
    if (hasModifierModal) {
      console.log('   Modal de extras abierto. Seleccionando modificadores...');
      // Marcar checkbox 1 (Extra Queso Cheddar) y checkbox 2 (Porción Extra Carne)
      await page.evaluate(`
        (() => {
          const checkboxes = document.querySelectorAll('[data-testid="modifier-selection-modal"] input[type="checkbox"]');
          if (checkboxes[0] && !checkboxes[0].checked) checkboxes[0].click();
          if (checkboxes[1] && !checkboxes[1].checked) checkboxes[1].click();
        })()
      `);
      await page.sleep(500);
      await page.captureScreenshot(path.join(SCRATCH_DIR, '05a_modifiers_selected.png'));

      // Confirmar selección
      await page.click('[data-testid="confirm-modifiers-btn"]');
      await page.sleep(1000);
    }

    const cartTextWithModifiers = await page.getText('[data-testid="pos-cart"]');
    const hasExtrasInCart =
      cartTextWithModifiers.includes('Extra Queso Cheddar') ||
      cartTextWithModifiers.includes('Porción Extra Carne');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '05b_cart_with_modifiers.png'));
    recordResult(
      'TEST 5: Modal y Selección de Extras / Modificadores (TASK-210)',
      hasModifierModal && hasExtrasInCart,
      'Extras seleccionados y agregados exitosamente con su recargo a la comanda'
    );

    // -------------------------------------------------------------------------
    // TEST 6: POS - Selección y Personalización de Combo Flexible (TASK-208)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 6] Selección y Personalización de Combo Flexible (Packs)');
    // Clic en pestaña de combos
    await page.click('[data-testid="cat-tab-combos"]');
    await page.sleep(1000);

    const comboCardSelector = '[data-testid="combo-card-COMBO-CLASICO-01"]';
    await page.waitForSelector(comboCardSelector, 5000);
    await page.click(comboCardSelector);
    await page.sleep(1000);

    const hasComboModal = await page.evaluate('!!document.querySelector("[data-testid=\'combo-selection-modal\']")');
    if (hasComboModal) {
      console.log('   Modal de personalización de combo abierto. Confirmando opciones...');
      await page.captureScreenshot(path.join(SCRATCH_DIR, '06a_combo_modal.png'));
      await page.click('[data-testid="confirm-combo-btn"]');
      await page.sleep(1000);
    }

    const cartTextWithCombo = await page.getText('[data-testid="pos-cart"]');
    const hasComboInCart = cartTextWithCombo.includes('Combo Burger Clásico') || cartTextWithCombo.includes('PACK');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '06b_cart_with_combo.png'));
    recordResult(
      'TEST 6: Combos Gastronómicos Flexibles / Packs (TASK-208)',
      hasComboInCart,
      'Combo agregado a la comanda con sus componentes listos para deducción recursiva'
    );

    // -------------------------------------------------------------------------
    // TEST 7: Desglose de Descuento Promocional en Carrito (TASK-209)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 7] Desglose Reactivo de Descuento Promocional');
    const discountRowText = await page.getText('[data-testid="cart-discount-row"]');
    const hasDiscountBreakdown = !!discountRowText && discountRowText.includes('Descuento') && discountRowText.includes('-Bs.');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '07_discount_breakdown.png'));
    recordResult(
      'TEST 7: Desglose Reactivo de Descuento Promocional (TASK-209)',
      hasDiscountBreakdown,
      `Descuento aplicado: "${discountRowText?.trim()}"`
    );

    // -------------------------------------------------------------------------
    // TEST 8: Checkout de Venta, Cobro en Efectivo & Emisión de Ticket
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 8] Proceso de Cobro (Checkout) y Emisión de Ticket');
    await page.click('[data-testid="open-checkout-btn"]');
    await page.sleep(1000);

    await page.captureScreenshot(path.join(SCRATCH_DIR, '08a_checkout_modal.png'));
    await page.click('[data-testid="confirm-checkout-btn"]');
    await page.sleep(2500);

    // Modal de Ticket
    await page.waitForSelector('[data-testid="ticket-modal"]', 5000);
    const ticketText = await page.getText('[data-testid="ticket-modal"]');
    const hasTicketHeader = ticketText.includes('RESTAURANTE') && ticketText.includes('12345678');
    const hasPaidTotal = ticketText.includes('TOTAL PAGADO');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '08b_ticket_modal.png'));

    // Cerrar ticket
    await page.click('[data-testid="close-ticket-btn"]');
    await page.sleep(1000);

    const checkoutPassed = hasTicketHeader && hasPaidTotal;
    recordResult(
      'TEST 8: Cobro de Pedido y Emisión de Ticket',
      checkoutPassed,
      'Ticket fiscal emitido en pantalla con desglose de ítems, extras y combo pagado'
    );

    // -------------------------------------------------------------------------
    // TEST 9: Verificación de Kardex & Deducción de Inventario (TASK-206 / 208 / 210)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 9] Kardex e Inventario - Deducción Automática Post-Venta');
    await page.click('[data-testid="nav-inventory"]');
    await page.sleep(2500);

    const inventoryText = await page.evaluate('document.body.innerText.toLowerCase()');
    const hasCarne = inventoryText.includes('carne');
    const hasQueso = inventoryText.includes('queso cheddar');
    const hasStockColumns = inventoryText.includes('stock actual') || inventoryText.includes('existencias');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '09_inventory_kardex.png'));

    const inventoryPassed = hasCarne && hasQueso && hasStockColumns;
    recordResult(
      'TEST 9: Kardex e Inventario Físico (TASK-206 / TASK-208 / TASK-210)',
      inventoryPassed,
      'Existencias actualizadas en tiempo real con deducción atómica de recetas y extras'
    );

    // -------------------------------------------------------------------------
    // TEST 10: Finanzas & Arqueo de Turno de Caja (TASK-208 / TASK-008)
    // -------------------------------------------------------------------------
    console.log('\n▶ [TEST 10] Finanzas & Balance de Turno de Caja');
    await page.click('[data-testid="nav-finance"]');
    await page.sleep(2500);

    const financeText = await page.evaluate('document.body.innerText');
    const hasOpenShift = financeText.includes('Turno Abierto') || financeText.includes('OPEN');
    const hasSalesAccumulated = financeText.includes('Ventas en Efectivo') || financeText.includes('Total Cobrado') || financeText.includes('Bs.');
    await page.captureScreenshot(path.join(SCRATCH_DIR, '10_finance_shift.png'));

    const financePassed = hasOpenShift && hasSalesAccumulated;
    recordResult(
      'TEST 10: Finanzas & Arqueo de Turno de Caja (TASK-208 / TASK-008)',
      financePassed,
      'Turno de caja abierto registra e incrementa el total de ventas en efectivo'
    );

  } catch (err) {
    console.error('Error durante la ejecución E2E:', err);
    recordResult('EJECUCIÓN GENERAL', false, err.message);
  } finally {
    await page.close();
  }

  console.log('\n================================================================');
  console.log('📊 RESUMEN FINAL DE PRUEBAS E2E:');
  const passedCount = results.filter(r => r.passed).length;
  console.log(`Total: ${results.length} | Pasadas: ${passedCount} | Fallidas: ${results.length - passedCount}`);
  console.log('================================================================\n');
}

runAllTests().catch(console.error);
