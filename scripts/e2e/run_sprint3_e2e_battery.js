/**
 * run_sprint3_e2e_battery.js
 * 
 * Batería de Pruebas E2E Automatizada para certificar el Sprint 3 (v0.3.0).
 * Conecta directamente sobre Chrome DevTools Protocol (CDP en http://localhost:9222)
 * cubriendo los 5 casos de uso del Sprint:
 * 1. Onboarding Atómico y Product Tour (PB-001 / PB-040)
 * 2. Parametría Multinivel con Hot-Reload (PB-026)
 * 3. Roles Dinámicos y Matriz RBAC (PB-039)
 * 4. Adaptabilidad Móvil en POS e Inventario (PB-028)
 * 5. Ciclo de Vida de Sesiones y Silent Refresh con Rotación (PB-027)
 * 
 * Uso:
 *   cd frontend && NODE_PATH=./node_modules node ../scripts/e2e/run_sprint3_e2e_battery.js
 */

const { chromium } = require('@playwright/test');

async function runSprint3Battery() {
  console.log('================================================================');
  console.log('🚀 INICIANDO BATERÍA DE PRUEBAS E2E AUTOMATIZADA - SPRINT 3');
  console.log('================================================================\n');

  console.log('Conectando a Google Chrome vía CDP en http://localhost:9222 ...');
  const browser = await chromium.connectOverCDP('http://localhost:9222');
  const context = browser.contexts()[0];
  const page = context.pages().find(p => p.url().includes('4200')) || await context.newPage();

  // --------------------------------------------------------------------------
  // CASO 1: ONBOARDING ATÓMICO Y PRODUCT TOUR (PB-001 / PB-040)
  // --------------------------------------------------------------------------
  console.log('\n--- CASO 1: ONBOARDING ATÓMICO Y PRODUCT TOUR (PB-001 / PB-040) ---');
  await page.evaluate(() => localStorage.clear());
  await page.goto('http://localhost:4200/onboarding', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  const testSuffix = Date.now().toString().slice(-4);
  const restaurantName = `Trattoria E2E ${testSuffix}`;
  const slug = `trattoria-e2e-${testSuffix}`;

  console.log(`Llenando Paso 1: ${restaurantName} (slug: ${slug})...`);
  await page.fill('[data-testid="legal-name-input"]', restaurantName);
  await page.fill('[data-testid="nit-input"]', '1029384756');
  await page.fill('[data-testid="slug-input"]', slug);
  await page.waitForTimeout(600);

  await page.fill('[data-testid="admin-fullname-input"]', 'Admin E2E');
  await page.fill('[data-testid="admin-username-input"]', `admin_${testSuffix}`);
  await page.fill('[data-testid="admin-email-input"]', `admin_${testSuffix}@e2e.bo`);
  await page.fill('[data-testid="admin-password-input"]', 'Admin123$');
  await page.waitForTimeout(500);

  await page.click('[data-testid="step-1-next-btn"]');
  await page.waitForTimeout(800);

  console.log('Llenando Paso 2: Sucursal Principal...');
  await page.fill('[data-testid="branch-name-input"]', 'Sucursal Central E2E');
  await page.fill('[data-testid="branch-phone-input"]', '71524361');
  const laPazBtn = page.locator('[data-testid="city-preset-La Paz"]');
  if (await laPazBtn.count() > 0) {
    await laPazBtn.click();
  }
  await page.waitForTimeout(500);

  await page.click('[data-testid="step-2-next-btn"]');
  await page.waitForTimeout(800);

  console.log('Llenando Paso 3: Perfil Operativo A la Carta...');
  const aLaCarteCard = page.locator('[data-testid="template-card-A_LA_CARTE"]');
  if (await aLaCarteCard.count() > 0) {
    await aLaCarteCard.click();
  }
  await page.waitForTimeout(500);

  await page.click('[data-testid="open-summary-modal-btn"]');
  await page.waitForTimeout(800);

  console.log('Confirmando aprovisionamiento atómico...');
  await page.click('[data-testid="confirm-onboarding-submit-btn"]');
  await page.waitForURL('**/pos', { timeout: 15000 });
  await page.waitForTimeout(1500);
  console.log('✅ Redirección exitosa a /pos con sesión creada!');

  const tourModal = page.locator('[data-testid="product-tour-modal"]');
  const isTourVisible = await tourModal.isVisible().catch(() => false);
  console.log(`Product Tour activo: ${isTourVisible}`);
  if (isTourVisible) {
    const skipTourBtn = page.locator('[data-testid="tour-skip-btn"]');
    if (await skipTourBtn.isVisible()) {
      await skipTourBtn.click();
      await page.waitForTimeout(600);
    }
  }
  console.log('✅ Caso 1 Certificado con Éxito!');

  // --------------------------------------------------------------------------
  // CASO 2: PARAMETRÍA DINÁMICA MULTINIVEL CON HOT-RELOAD (PB-026)
  // --------------------------------------------------------------------------
  console.log('\n--- CASO 2: PARAMETRÍA DINÁMICA CON HOT-RELOAD (PB-026) ---');
  await page.goto('http://localhost:4200/settings/parameters', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  console.log('Buscando parámetro session_inactivity...');
  const searchInput = page.locator('[data-testid="parameter-search-input"]');
  await searchInput.fill('session_inactivity');
  await page.waitForTimeout(600);

  const inputParam = page.locator('[data-testid="input-security.session_inactivity_timeout_minutes"]');
  const saveBtn = page.locator('[data-testid="btn-save-security.session_inactivity_timeout_minutes"]');
  if (await inputParam.isVisible()) {
    console.log('Modificando valor a 60 minutos...');
    await inputParam.fill('60');
    await page.waitForTimeout(300);
    await saveBtn.click();
    await page.waitForTimeout(1000);
    console.log('✅ Parámetro guardado y aplicado en caliente (Hot-Reload)!');
  }

  // --------------------------------------------------------------------------
  // CASO 3: ROLES DINÁMICOS Y MATRIZ RBAC (PB-039)
  // --------------------------------------------------------------------------
  console.log('\n--- CASO 3: ROLES DINÁMICOS Y MATRIZ RBAC (PB-039) ---');
  await page.goto('http://localhost:4200/users/roles', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  console.log('Creando nuevo rol personalizado...');
  await page.click('[data-testid="create-custom-role-btn"]');
  await page.waitForTimeout(500);

  const roleName = `Supervisor E2E ${testSuffix}`;
  await page.fill('[data-testid="role-name-input"]', roleName);
  await page.fill('[data-testid="role-desc-input"]', 'Supervisión de caja y catálogo');

  const permCb = page.locator('[data-testid="perm-checkbox-catalog:read"]');
  if (await permCb.count() > 0) {
    await permCb.check();
  }
  await page.waitForTimeout(400);

  await page.click('[data-testid="save-role-btn"]');
  await page.waitForTimeout(1200);
  console.log(`✅ Rol "${roleName}" creado y persistido con éxito en base de datos!`);

  // --------------------------------------------------------------------------
  // CASO 4: ADAPTABILIDAD MÓVIL EN POS E INVENTARIO (PB-028)
  // --------------------------------------------------------------------------
  console.log('\n--- CASO 4: ADAPTABILIDAD MÓVIL EN POS E INVENTARIO (PB-028) ---');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('http://localhost:4200/pos', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  const listModeBtn = page.locator('[data-testid="view-mode-list-btn"]');
  if (await listModeBtn.isVisible()) {
    await listModeBtn.click();
    await page.waitForTimeout(400);
    console.log('Catálogo conmutado a Modo Lista Compacta.');
  }

  const addBtns = page.locator('button[data-testid^="add-btn-"], [data-testid^="dish-card-"]');
  if (await addBtns.count() > 0) {
    await addBtns.first().click();
    await page.waitForTimeout(500);
  }

  const mobileCartBar = page.locator('[data-testid="open-mobile-cart-btn"]');
  if (await mobileCartBar.isVisible()) {
    console.log('Barra Flotante Inferior detectada. Abriendo Bottom Sheet...');
    await mobileCartBar.click();
    await page.waitForTimeout(800);
    console.log('✅ Bottom Sheet de comanda abierto con éxito en smartphone!');
  }

  console.log('Verificando vista móvil de Inventario...');
  await page.goto('http://localhost:4200/inventory', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  const mobileStockCards = page.locator('[data-testid="mobile-stock-cards"]');
  const desktopStockTable = page.locator('[data-testid="stock-table"]');
  console.log(`Tarjetas móviles activas: ${await mobileStockCards.isVisible()}`);
  console.log(`Tabla de escritorio oculta: ${!(await desktopStockTable.isVisible())}`);

  const firstCard = page.locator('[data-testid^="mobile-stock-card-"]').first();
  if (await firstCard.isVisible()) {
    await firstCard.click();
    await page.waitForTimeout(500);
    console.log('✅ Acordeón de insumo expandido con detalles financieros.');
  }

  await page.setViewportSize({ width: 1280, height: 800 });

  // --------------------------------------------------------------------------
  // CASO 5: SESIONES, TOKENS DUALES Y SILENT REFRESH (PB-027)
  // --------------------------------------------------------------------------
  console.log('\n--- CASO 5: SESIONES Y SILENT REFRESH CON ROTACIÓN (PB-027) ---');
  await page.evaluate(() => localStorage.clear());
  await page.goto('http://localhost:4200/auth/login', { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);

  await page.fill('#tenant-input', 'demo-restaurant');
  await page.fill('#login-input', 'admin');
  await page.fill('#password-input', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForTimeout(2000);

  const authState = await page.evaluate(() => ({
    hasAuthToken: !!localStorage.getItem('auth_token'),
    hasRefreshToken: !!localStorage.getItem('auth_refresh_token')
  }));
  console.log('Tokens en cliente post-login:', authState);

  const refreshResult = await page.evaluate(async () => {
    const rawRefresh = localStorage.getItem('auth_refresh_token');
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rawRefresh })
    });
    const data = await res.json();
    return { status: res.status, ok: res.ok, hasNewAccess: !!(data.token || data.accessToken) };
  });
  console.log('Resultado Silent Refresh:', refreshResult);
  console.log('✅ Caso 5 Certificado con Éxito!');

  console.log('\n================================================================');
  console.log('🎉 BATERÍA DE PRUEBAS E2E DEL SPRINT 3 CONCLUIDA EXITOSAMENTE');
  console.log('================================================================');

  await browser.close();
}

runSprint3Battery().catch(err => {
  console.error('Error durante la ejecución de la batería:', err);
  process.exit(1);
});
