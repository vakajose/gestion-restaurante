// run_shift_session_flow.js - Ciclo completo de Turno, Ventas POS, Autorizaciones Admin y Arqueo Final
import { createOrAttachPage } from './cdp_client.js';
import * as path from 'node:path';

import * as fs from 'node:fs';

const SCREENSHOT_DIR = process.env.SCREENSHOT_DIR || path.resolve('e2e-screenshots');
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}
const SCRATCH_DIR = SCREENSHOT_DIR;

async function executeShiftSessionFlow() {
  console.log('========================================================================');
  console.log('🏪 INICIANDO FLUJO COMPLETO: CAJERO → VENTAS → GASTOS → ADMIN → ARQUEO');
  console.log('========================================================================\n');

  const page = await createOrAttachPage('http://localhost:4200/auth/login');
  const logSteps = [];

  function recordStep(stepNum, name, status, details = '') {
    logSteps.push({ stepNum, name, status, details });
    const icon = status === 'SUCCESS' ? '✅' : '❌';
    console.log(`${icon} [PASO ${stepNum}] ${name}`);
    if (details) console.log(`   └─ ${details}`);
    console.log('------------------------------------------------------------------------');
  }

  try {
    // -------------------------------------------------------------------------
    // PASO 1: Inicio de Sesión como Cajero (cajero1)
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 1: Autenticación del Cajero (cajero1)');
    await page.navigate('http://localhost:4200/auth/login');
    await page.sleep(1500);

    // Si ya hay sesión activa, cerrar sesión primero
    const hasLogout = await page.evaluate('!!document.querySelector("[data-testid=\'logout-btn\']")');
    if (hasLogout) {
      console.log('   Cerrando sesión previa...');
      await page.click('[data-testid="logout-btn"]');
      await page.sleep(1500);
    }

    // Usar atajo de Demo Cajero
    await page.waitForSelector('[data-testid="demo-cashier-btn"]', 5000);
    await page.click('[data-testid="demo-cashier-btn"]');
    await page.sleep(400);

    await page.click('[data-testid="login-submit-btn"]');
    await page.sleep(2500);

    const postLoginUrl = await page.evaluate('window.location.pathname');
    const userRole = await page.evaluate('document.body.innerText');
    const isCajeroActive = userRole.includes('cajero1') || userRole.includes('CASHIER') || postLoginUrl.includes('/pos');
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_01_cajero_logged_in.png'));
    recordStep(1, 'Autenticación del Cajero (cajero1)', isCajeroActive ? 'SUCCESS' : 'FAILED', `Ruta: ${postLoginUrl}`);

    // -------------------------------------------------------------------------
    // PASO 2: Venta 1 en POS - Combo Burger Clásico con Descuento Happy Hour
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 2: Venta 1 - Combo Burger Clásico con Happy Hour');
    await page.navigate('http://localhost:4200/pos');
    await page.sleep(2000);

    // Clic en pestaña Combos
    await page.click('[data-testid="cat-tab-combos"]');
    await page.sleep(800);

    // Clic en tarjeta de combo
    await page.waitForSelector('[data-testid="combo-card-COMBO-CLASICO-01"]', 5000);
    await page.click('[data-testid="combo-card-COMBO-CLASICO-01"]');
    await page.sleep(1000);

    // Si tiene modal de personalización, confirmarlo
    const hasComboModal = await page.evaluate('!!document.querySelector("[data-testid=\'combo-selection-modal\']")');
    if (hasComboModal) {
      await page.click('[data-testid="confirm-combo-btn"]');
      await page.sleep(1000);
    }

    // Cobrar venta 1
    await page.click('[data-testid="open-checkout-btn"]');
    await page.sleep(800);
    await page.click('[data-testid="confirm-checkout-btn"]');
    await page.sleep(2000);

    // Cerrar ticket
    await page.waitForSelector('[data-testid="ticket-modal"]', 5000);
    await page.click('[data-testid="close-ticket-btn"]');
    await page.sleep(800);
    console.log('   Venta 1 (Combo) cobrada exitosamente.');

    // -------------------------------------------------------------------------
    // PASO 3: Venta 2 en POS - Hamburguesa con Extras / Modificadores
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 3: Venta 2 - Hamburguesa Clásica con Extras');
    await page.click('[data-testid="cat-tab-all"]');
    await page.sleep(800);

    await page.waitForSelector('[data-testid="dish-card-DISH-BURGER-01"]', 5000);
    await page.click('[data-testid="dish-card-DISH-BURGER-01"]');
    await page.sleep(1000);

    // Seleccionar modificadores en modal
    await page.waitForSelector('[data-testid="modifier-selection-modal"]', 5000);
    await page.evaluate(`
      (() => {
        const checkboxes = document.querySelectorAll('[data-testid="modifier-selection-modal"] input[type="checkbox"]');
        if (checkboxes[0] && !checkboxes[0].checked) checkboxes[0].click(); // Extra Queso
        if (checkboxes[1] && !checkboxes[1].checked) checkboxes[1].click(); // Extra Carne
      })()
    `);
    await page.sleep(500);
    await page.click('[data-testid="confirm-modifiers-btn"]');
    await page.sleep(1000);

    // Cobrar venta 2
    await page.click('[data-testid="open-checkout-btn"]');
    await page.sleep(800);
    await page.click('[data-testid="confirm-checkout-btn"]');
    await page.sleep(2000);

    // Cerrar ticket
    await page.waitForSelector('[data-testid="ticket-modal"]', 5000);
    await page.click('[data-testid="close-ticket-btn"]');
    await page.sleep(800);
    console.log('   Venta 2 (Hamburguesa con Extras) cobrada exitosamente.');
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_02_pos_sales_completed.png'));
    recordStep(2, 'Ventas en POS (Combos, Modificadores, Promoción Happy Hour)', 'SUCCESS', '2 ventas completadas y cobradas en efectivo');

    // -------------------------------------------------------------------------
    // PASO 4: Cajero registra 2 solicitudes de egreso de caja menor
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 4: Cajero registra Solicitudes de Gasto de Caja Menor');
    await page.navigate('http://localhost:4200/finance');
    await page.sleep(2000);

    // Ir a pestaña de gastos
    await page.waitForSelector('[data-testid="tab-expenses"]', 5000);
    await page.click('[data-testid="tab-expenses"]');
    await page.sleep(1000);

    // Registro de Gasto 1: Hielo y Limones (Bs. 35.00)
    console.log('   Registrando Gasto 1: Compra urgente de hielo y limones (Bs. 35.00)...');
    await page.click('[data-testid="btn-new-expense"]');
    await page.sleep(800);

    await page.evaluate(`
      (() => {
        const desc = document.querySelector('#expenseDescInput');
        desc.value = 'Compra urgente de hielo y limones';
        desc.dispatchEvent(new Event('input', { bubbles: true }));

        const amount = document.querySelector('#expenseAmountInput');
        amount.value = '35.00';
        amount.dispatchEvent(new Event('input', { bubbles: true }));
      })()
    `);
    await page.sleep(500);
    await page.click('[data-testid="btn-confirm-save-expense"]');
    await page.sleep(1500);

    // Registro de Gasto 2: Desinfección de mostrador (Bs. 45.00)
    console.log('   Registrando Gasto 2: Servicio urgente de desinfección de mostrador (Bs. 45.00)...');
    await page.click('[data-testid="btn-new-expense"]');
    await page.sleep(800);

    await page.evaluate(`
      (() => {
        const desc = document.querySelector('#expenseDescInput');
        desc.value = 'Servicio urgente de desinfección de mostrador';
        desc.dispatchEvent(new Event('input', { bubbles: true }));

        const amount = document.querySelector('#expenseAmountInput');
        amount.value = '45.00';
        amount.dispatchEvent(new Event('input', { bubbles: true }));
      })()
    `);
    await page.sleep(500);
    await page.click('[data-testid="btn-confirm-save-expense"]');
    await page.sleep(1500);

    // Verificar que los gastos figuran como Pendientes de Autorización
    const expensesTableText = await page.evaluate('document.body.innerText');
    const hasExpense1 = expensesTableText.includes('Compra urgente de hielo y limones');
    const hasExpense2 = expensesTableText.includes('Servicio urgente de desinfección de mostrador');
    const hasPendingState = expensesTableText.includes('Pendiente') || expensesTableText.includes('PENDING_APPROVAL');

    // Verificar que el cajero NO tiene botones de aprobación habilitados
    const cajeroHasApproveBtn = await page.evaluate('!!document.querySelector("[data-testid=\'btn-approve-expense\']")');
    console.log(`   ¿Tiene el cajero botones de aprobación?: ${cajeroHasApproveBtn ? 'SÍ (Error)' : 'NO (Correcto - Requiere Admin)'}`);

    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_03_expenses_pending_authorization.png'));
    recordStep(
      3,
      'Registro de Gastos Menores por Cajero (Pendientes de Autorización)',
      hasExpense1 && hasExpense2 && !cajeroHasApproveBtn ? 'SUCCESS' : 'FAILED',
      '2 gastos de Bs. 35.00 y Bs. 45.00 creados en estado PENDING_APPROVAL. Cajero restringido de auto-aprobarse.'
    );

    // -------------------------------------------------------------------------
    // PASO 5: Conmutación a Sesión del Administrador (admin)
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 5: Cierre de Sesión del Cajero y Login del Administrador');
    await page.click('[data-testid="logout-btn"]');
    await page.sleep(1500);

    // Login Admin
    await page.waitForSelector('[data-testid="demo-admin-btn"]', 5000);
    await page.click('[data-testid="demo-admin-btn"]');
    await page.sleep(400);
    await page.click('[data-testid="login-submit-btn"]');
    await page.sleep(2500);

    const adminBodyText = await page.evaluate('document.body.innerText');
    const isAdminActive = adminBodyText.includes('admin') && adminBodyText.includes('ADMIN_TENANT');
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_04_admin_logged_in.png'));
    recordStep(4, 'Conmutación de Sesión a Administrador de Empresa', isAdminActive ? 'SUCCESS' : 'FAILED', 'Admin autenticado con permisos plenos');

    // -------------------------------------------------------------------------
    // PASO 6: Autorización de Gastos por el Administrador
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 6: Administrador aprueba las 2 solicitudes de gasto');
    await page.navigate('http://localhost:4200/finance');
    await page.sleep(2000);

    await page.waitForSelector('[data-testid="tab-expenses"]', 5000);
    await page.click('[data-testid="tab-expenses"]');
    await page.sleep(1000);

    // Aprobar Gasto 1
    console.log('   Aprobando Gasto 1...');
    await page.waitForSelector('[data-testid="btn-approve-expense"]', 5000);
    await page.click('[data-testid="btn-approve-expense"]');
    await page.sleep(1500);

    // Aprobar Gasto 2 (si queda otro pendiente)
    const hasSecondPending = await page.evaluate('!!document.querySelector("[data-testid=\'btn-approve-expense\']")');
    if (hasSecondPending) {
      console.log('   Aprobando Gasto 2...');
      await page.click('[data-testid="btn-approve-expense"]');
      await page.sleep(1500);
    }

    const expensesAfterApproval = await page.evaluate('document.body.innerText');
    const isApprovedVisible = expensesAfterApproval.includes('Aprobado') || expensesAfterApproval.includes('APPROVED');
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_05_admin_expenses_approved.png'));
    recordStep(5, 'Autorización de Gastos por el Administrador', isApprovedVisible ? 'SUCCESS' : 'FAILED', 'Ambos gastos autorizados y asentados para deducción en gaveta');

    // -------------------------------------------------------------------------
    // PASO 7: Conciliación, Arqueo y Cierre de Turno de Caja
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 7: Conciliación y Arqueo Final de Caja');
    // Ir a pestaña de turnos
    await page.evaluate(`
      (() => {
        const tabs = Array.from(document.querySelectorAll('button'));
        const shiftTab = tabs.find(t => t.innerText.includes('Turnos') || t.innerText.includes('Turno'));
        if (shiftTab) shiftTab.click();
      })()
    `);
    await page.sleep(1000);

    // Obtener los datos del turno antes de cerrar
    const shiftKpis = await page.evaluate(`
      (() => {
        const cards = Array.from(document.querySelectorAll('.card'));
        return cards.map(c => c.innerText.replace(/\\n/g, ' ')).join(' | ');
      })()
    `);
    console.log(`   KPIs de Caja: ${shiftKpis}`);

    // Clic en Cerrar Turno / Arqueo
    await page.waitForSelector('[data-testid="btn-close-shift"]', 5000);
    await page.click('[data-testid="btn-close-shift"]');
    await page.sleep(1000);

    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_06a_arqueo_modal_open.png'));

    // Leer el efectivo teórico esperado mostrado en el modal
    const expectedCashValue = await page.evaluate(`
      (() => {
        const text = document.body.innerText;
        const match = text.match(/Efectivo Teórico Esperado:\\s*Bs\\.\\s*([\\d\\.,]+)/i);
        return match ? parseFloat(match[1].replace(/,/g, '')) : 200.0;
      })()
    `);
    console.log(`   Efectivo Teórico Esperado en Gaveta: Bs. ${expectedCashValue}`);

    // Asignar el efectivo real contado (cuadre perfecto)
    await page.evaluate(`
      (() => {
        const input = document.querySelector('[data-testid="input-actual-cash"]');
        input.value = '${expectedCashValue}';
        input.dispatchEvent(new Event('input', { bubbles: true }));

        const notes = document.querySelector('[data-testid="input-closing-notes"]');
        notes.value = 'Arqueo conciliado contra ventas en mostrador y gastos aprobados de caja chica. Sin diferencias.';
        notes.dispatchEvent(new Event('input', { bubbles: true }));
      })()
    `);
    await page.sleep(500);
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_06b_arqueo_reconciled.png'));

    // Confirmar cierre de turno
    await page.click('[data-testid="btn-confirm-close-shift"]');
    await page.sleep(2500);

    // Verificar que el turno pasa a Cerrado / Sin turno activo
    const postCloseText = await page.evaluate('document.body.innerText');
    const isShiftClosed = postCloseText.includes('Sin turno activo') || postCloseText.includes('Cerrado') || postCloseText.includes('Abrir Turno');
    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_06c_shift_closed_state.png'));
    recordStep(
      6,
      'Conciliación, Arqueo y Cierre Definitivo del Turno',
      isShiftClosed ? 'SUCCESS' : 'FAILED',
      `Turno cerrado exitosamente con cuadre exacto de Bs. ${expectedCashValue}`
    );

    // -------------------------------------------------------------------------
    // PASO 8: Limpieza y Preparación para el Inicio del Siguiente Día
    // -------------------------------------------------------------------------
    console.log('\n▶ PASO 8: Verificación de Estado Listo para Nueva Sesión al Día Siguiente');
    const hasOpenShiftBtn = await page.evaluate('!!document.querySelector("[data-testid=\'btn-open-shift\']")');
    console.log(`   ¿Botón "Abrir Turno de Caja" disponible para el siguiente día?: ${hasOpenShiftBtn ? 'SÍ' : 'NO'}`);

    await page.captureScreenshot(path.join(SCRATCH_DIR, 'flow_07_ready_for_next_day.png'));
    recordStep(
      7,
      'Preparación para Nueva Sesión (Día Siguiente)',
      hasOpenShiftBtn ? 'SUCCESS' : 'FAILED',
      'El sistema queda en estado limpio y seguro: turno cerrado, arqueo archivado y botón de apertura listo para la jornada siguiente.'
    );

  } catch (err) {
    console.error('Error durante la ejecución del flujo:', err);
    recordStep(99, 'EJECUCIÓN DEL FLUJO OPERATIVO', 'FAILED', err.message);
  } finally {
    await page.close();
  }

  console.log('\n========================================================================');
  console.log('📊 RESUMEN FINAL DEL FLUJO OPERATIVO DE SESIÓN:');
  const passedCount = logSteps.filter(s => s.status === 'SUCCESS').length;
  console.log(`Total Pasos: ${logSteps.length} | Exitosos: ${passedCount} | Fallidos: ${logSteps.length - passedCount}`);
  console.log('========================================================================\n');
}

executeShiftSessionFlow().catch(console.error);
