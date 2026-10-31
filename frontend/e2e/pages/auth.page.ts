import { Page, expect } from '@playwright/test';

export class AuthPage {
  constructor(private readonly page: Page) {}

  async gotoLogin(): Promise<void> {
    await this.page.goto('/auth/login');
    await expect(this.page.locator('[data-testid="login-submit-btn"]')).toBeVisible();
  }

  async loginAsCashier(): Promise<void> {
    await this.gotoLogin();
    await this.page.locator('[data-testid="demo-cashier-btn"]').click();
    await this.page.locator('[data-testid="login-submit-btn"]').click();
    await this.page.waitForURL('**/pos', { timeout: 15000 });
  }

  async loginAsAdmin(): Promise<void> {
    await this.gotoLogin();
    await this.page.locator('[data-testid="demo-admin-btn"]').click();
    await this.page.locator('[data-testid="login-submit-btn"]').click();
    // Admin redirects to company or default dashboard
    await this.page.waitForURL((url) => !url.pathname.includes('/auth/login'), { timeout: 15000 });
  }
}
