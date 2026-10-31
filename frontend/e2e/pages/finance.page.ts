import { Page, Locator, expect } from '@playwright/test';

export class FinancePage {
  readonly tabShifts: Locator;
  readonly tabExpenses: Locator;
  readonly btnApproveExpense: Locator;
  readonly btnOpenConsolidate: Locator;
  readonly inputConsolidationNotes: Locator;

  constructor(private readonly page: Page) {
    this.tabShifts = this.page.locator('[data-testid="tab-shifts"]');
    this.tabExpenses = this.page.locator('[data-testid="tab-expenses"]');
    this.btnApproveExpense = this.page.locator('[data-testid="btn-approve-expense"]');
    this.btnOpenConsolidate = this.page.locator('[data-testid="btn-open-consolidate"]');
    this.inputConsolidationNotes = this.page.locator('[data-testid="input-consolidation-notes"]');
  }

  async goto(): Promise<void> {
    await this.page.goto('/finance');
    await expect(this.page.locator('h1:has-text("Finanzas")')).toBeVisible({ timeout: 15000 });
  }

  async selectTab(tab: 'shifts' | 'expenses'): Promise<void> {
    if (tab === 'shifts') {
      await this.tabShifts.click();
    } else {
      await this.tabExpenses.click();
    }
  }

  async approveExpense(expenseDesc?: string): Promise<void> {
    await this.selectTab('expenses');
    let btn = this.page.locator('button:has-text("Aprobar"), [data-testid="btn-approve-expense"]').first();
    if (expenseDesc) {
      const row = this.page.locator(`tr:has-text("${expenseDesc}")`);
      btn = row.locator('button:has-text("Aprobar"), [data-testid="btn-approve-expense"]');
    }
    await expect(btn).toBeVisible({ timeout: 10000 });
    await btn.click();
  }

  async consolidateShift(notes = 'Consolidación certificada E2E'): Promise<void> {
    await this.selectTab('shifts');
    // Ensure we are viewing closed shifts
    const filterClosedBtn = this.page.locator('[data-testid="filter-shifts-closed"]');
    if (await filterClosedBtn.isVisible()) {
      await filterClosedBtn.click();
    }

    const consolidateBtn = this.page.locator('table [data-testid="btn-open-consolidate"], table button:has-text("Consolidar")').first();
    await expect(consolidateBtn).toBeVisible({ timeout: 10000 });
    await consolidateBtn.click();

    await expect(this.inputConsolidationNotes).toBeVisible({ timeout: 5000 });
    await this.inputConsolidationNotes.fill(notes);

    await this.page.locator('button:has-text("Consolidar Arqueo")').click();
    await expect(this.inputConsolidationNotes).not.toBeVisible({ timeout: 10000 });
  }
}
