import { Page, Locator, expect } from '@playwright/test';

export class CashControlPage {
  readonly btnOpenShift: Locator;
  readonly btnCloseShift: Locator;
  readonly btnNewExpense: Locator;
  readonly tabOrders: Locator;
  readonly tabExpenses: Locator;
  readonly inputInitialCash: Locator;
  readonly inputActualCash: Locator;
  readonly expectedDrawerCash: Locator;
  readonly inputExpenseDesc: Locator;
  readonly inputExpenseAmount: Locator;
  readonly filterCashierAll: Locator;
  readonly filterCashierMine: Locator;
  readonly selectPaymentFilter: Locator;

  constructor(private readonly page: Page) {
    this.btnOpenShift = this.page.locator('[data-testid="btn-open-shift"]');
    this.btnCloseShift = this.page.locator('[data-testid="btn-close-shift"]');
    this.btnNewExpense = this.page.locator('[data-testid="btn-new-expense"]');
    this.tabOrders = this.page.locator('[data-testid="tab-orders"]');
    this.tabExpenses = this.page.locator('[data-testid="tab-expenses"]');
    this.inputInitialCash = this.page.locator('[data-testid="input-initial-cash"]');
    this.inputActualCash = this.page.locator('[data-testid="input-actual-cash"]');
    this.expectedDrawerCash = this.page.locator('[data-testid="expected-drawer-cash"]');
    this.inputExpenseDesc = this.page.locator('[data-testid="input-expense-desc"]');
    this.inputExpenseAmount = this.page.locator('[data-testid="input-expense-amount"]');
    this.filterCashierAll = this.page.locator('[data-testid="filter-cashier-all"]');
    this.filterCashierMine = this.page.locator('[data-testid="filter-cashier-mine"]');
    this.selectPaymentFilter = this.page.locator('[data-testid="select-payment-filter"]');
  }

  async goto(): Promise<void> {
    await this.page.goto('/cash-control');
    await expect(this.page.locator('h1:has-text("Control de Caja")')).toBeVisible({ timeout: 15000 });
  }

  async openShift(initialCash = '200.00'): Promise<void> {
    await expect(this.btnOpenShift).toBeVisible({ timeout: 10000 });
    await this.btnOpenShift.click();
    await expect(this.inputInitialCash).toBeVisible();
    await this.inputInitialCash.fill(initialCash);
    await this.page.locator('button:has-text("Abrir Turno")').last().click();
    await expect(this.inputInitialCash).not.toBeVisible({ timeout: 10000 });
  }

  async registerExpense(desc: string, amount: string): Promise<void> {
    await expect(this.btnNewExpense).toBeVisible({ timeout: 10000 });
    await this.btnNewExpense.click();
    await expect(this.inputExpenseDesc).toBeVisible();
    await this.inputExpenseDesc.fill(desc);
    await this.inputExpenseAmount.fill(amount);
    await this.page.locator('button:has-text("Registrar Gasto")').last().click();
    await expect(this.inputExpenseDesc).not.toBeVisible({ timeout: 10000 });
  }

  async closeShift(actualCash?: string, notes = 'Arqueo de cierre E2E'): Promise<void> {
    await expect(this.btnCloseShift).toBeVisible({ timeout: 10000 });
    await this.btnCloseShift.click();
    await expect(this.inputActualCash).toBeVisible();

    let cashToFill = actualCash;
    if (!cashToFill) {
      await expect(this.expectedDrawerCash).toBeVisible();
      const text = (await this.expectedDrawerCash.textContent()) || '';
      const match = text.match(/\d+(\.\d+)?/);
      cashToFill = match ? match[0] : '200.00';
    }

    await this.inputActualCash.fill(cashToFill);
    if (notes) {
      await this.page.locator('textarea').fill(notes);
    }
    await this.page.locator('button:has-text("Cerrar y Asentar Arqueo")').last().click();
    await expect(this.inputActualCash).not.toBeVisible({ timeout: 15000 });
  }

  async filterByCashier(option: 'ALL' | 'MINE'): Promise<void> {
    if (option === 'ALL') {
      await this.filterCashierAll.click();
    } else {
      await this.filterCashierMine.click();
    }
  }

  async filterByPayment(method: string): Promise<void> {
    await this.selectPaymentFilter.selectOption(method);
  }
}
