import {expect, Page} from '@playwright/test';

/**
 * Navigates to the Users tab on the dashboard
 */
export async function navigateToUsersTab(page: Page): Promise<void> {
    await page.getByRole('button', {name: 'Users'}).click();
    await expect(page.locator('.users-table')).toBeVisible();
}

/**
 * Navigates to the Applications tab on the dashboard
 */
export async function navigateToApplicationsTab(page: Page): Promise<void> {
    await page.getByRole('button', {name: 'Applications'}).click();
    await expect(page.locator('.app-cards')).toBeVisible();
}
