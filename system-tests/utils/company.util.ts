import {expect, Page} from '@playwright/test';

/**
 * Creates a company during onboarding
 */
export async function createCompany(page: Page, companyName: string): Promise<void> {
    await expect(page.getByText('Create Your Company')).toBeVisible();
    await page.getByLabel('Company Name').fill(companyName);
    await page.getByRole('button', {name: 'Create Company'}).click();

    await expect(page.getByText('Create Your Company')).not.toBeVisible();
}

/**
 * Waits for the onboarding overlay to appear
 */
export async function expectOnboardingVisible(page: Page): Promise<void> {
    await expect(page.getByText('Create Your Company')).toBeVisible();
}
