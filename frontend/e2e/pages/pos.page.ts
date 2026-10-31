import { Page, Locator, expect } from '@playwright/test';

export class PosPage {
  readonly closedShiftState: Locator;
  readonly welcomeOpenShiftBtn: Locator;
  readonly openShiftModal: Locator;
  readonly initialCashInput: Locator;
  readonly selectMenuTemplate: Locator;
  readonly confirmOpenShiftBtn: Locator;
  readonly openCheckoutBtn: Locator;
  readonly checkoutModal: Locator;
  readonly confirmCheckoutBtn: Locator;
  readonly ticketModal: Locator;
  readonly closeTicketBtn: Locator;
  readonly ticketCashierName: Locator;
  readonly orderTypeDineIn: Locator;
  readonly orderTypeTakeout: Locator;
  readonly orderTypeDelivery: Locator;
  readonly inputCustomerName: Locator;

  constructor(private readonly page: Page) {
    this.closedShiftState = this.page.locator('[data-testid="pos-closed-shift-state"]');
    this.welcomeOpenShiftBtn = this.page.locator('[data-testid="welcome-open-shift-btn"]');
    this.openShiftModal = this.page.locator('[data-testid="open-shift-modal"]');
    this.initialCashInput = this.page.locator('[data-testid="initial-cash-input"]');
    this.selectMenuTemplate = this.page.locator('[data-testid="select-menu-template"]');
    this.confirmOpenShiftBtn = this.page.locator('[data-testid="confirm-open-shift-btn"]');
    this.openCheckoutBtn = this.page.locator('[data-testid="open-checkout-btn"]');
    this.checkoutModal = this.page.locator('h2:has-text("Cobrar Comanda de Venta")');
    this.confirmCheckoutBtn = this.page.locator('[data-testid="confirm-checkout-btn"]');
    this.ticketModal = this.page.locator('[data-testid="ticket-modal"]');
    this.closeTicketBtn = this.page.locator('[data-testid="close-ticket-btn"]');
    this.ticketCashierName = this.page.locator('[data-testid="ticket-cashier-name"]');
    this.orderTypeDineIn = this.page.locator('[data-testid="order-type-dine-in"]');
    this.orderTypeTakeout = this.page.locator('[data-testid="order-type-takeout"]');
    this.orderTypeDelivery = this.page.locator('[data-testid="order-type-delivery"]');
    this.inputCustomerName = this.page.locator('[data-testid="input-customer-name"]');
  }

  async goto(): Promise<void> {
    await this.page.goto('/pos');
    await expect(this.page.locator('h1, h2, h3, [data-testid="pos-cart"]').first()).toBeVisible({ timeout: 15000 });
  }

  async assertClosedShiftState(): Promise<void> {
    await expect(this.closedShiftState).toBeVisible();
    await expect(this.welcomeOpenShiftBtn).toBeVisible();
  }

  async openShift(initialCash = '200.00', templateIndex = 0): Promise<void> {
    if (await this.welcomeOpenShiftBtn.isVisible()) {
      await this.welcomeOpenShiftBtn.click();
    } else {
      const topBtn = this.page.locator('[data-testid="desktop-open-shift-btn"]');
      if (await topBtn.isVisible()) {
        await topBtn.click();
      }
    }
    await expect(this.openShiftModal).toBeVisible();
    await this.initialCashInput.fill(initialCash);

    if (await this.selectMenuTemplate.isVisible()) {
      const options = await this.selectMenuTemplate.locator('option').all();
      if (options.length > 1) {
        await this.selectMenuTemplate.selectOption({ index: templateIndex });
      }
    }

    await this.confirmOpenShiftBtn.click();
    await expect(this.openShiftModal).not.toBeVisible({ timeout: 10000 });
  }

  async addDishByCode(code: string): Promise<void> {
    // Check if modifiers modal is triggered or simple add
    const addBtn = this.page.locator(`[data-testid="add-btn-${code}"]`);
    await expect(addBtn).toBeVisible({ timeout: 10000 });
    await addBtn.click();

    const modifierModal = this.page.locator('[data-testid="modifier-selection-modal"]');
    if (await modifierModal.isVisible({ timeout: 1000 }).catch(() => false)) {
      await this.page.locator('[data-testid="confirm-modifiers-btn"]').click();
      await expect(modifierModal).not.toBeVisible();
    }
  }

  async openCheckout(): Promise<void> {
    await expect(this.openCheckoutBtn).toBeVisible();
    await expect(this.openCheckoutBtn).toBeEnabled();
    await this.openCheckoutBtn.click();
    await expect(this.checkoutModal).toBeVisible();
  }

  async checkoutExactCash(options?: { orderType?: 'DINE_IN' | 'TAKEOUT' | 'DELIVERY'; customerName?: string }): Promise<void> {
    if (options?.orderType === 'TAKEOUT') {
      await this.orderTypeTakeout.click();
    } else if (options?.orderType === 'DELIVERY') {
      await this.orderTypeDelivery.click();
    } else if (options?.orderType === 'DINE_IN') {
      await this.orderTypeDineIn.click();
    }

    if (options?.customerName) {
      await this.inputCustomerName.fill(options.customerName);
    }

    // Click exact amount button
    await this.page.locator('button:has-text("Exacto")').click();
    await this.confirmCheckoutBtn.click();

    // Verify ticket
    await expect(this.ticketModal).toBeVisible({ timeout: 10000 });
  }

  async closeTicket(): Promise<void> {
    await this.closeTicketBtn.click();
    await expect(this.ticketModal).not.toBeVisible();
  }
}
