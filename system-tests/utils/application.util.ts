import {expect, Page} from '@playwright/test';

/**
 * Creates a new application via the dashboard
 */
export async function createApplication(page: Page, appName: string): Promise<void> {
    // Click the add application card
    await page.locator('.app-card--add').click();

    // Wait for application creation form
    await expect(page.getByLabel('Application Name')).toBeVisible();

    // Fill in the application name
    await page.getByLabel('Application Name').fill(appName);

    // Submit the form
    await page.getByRole('button', {name: 'Create Application'}).click();

    // Wait for the overlay to close and app to appear
    await expect(page.getByLabel('Application Name')).not.toBeVisible();
    await expect(page.locator('.app-card').filter({hasText: appName})).toBeVisible();
}

/**
 * Clicks on an application card to navigate to its overview
 */
export async function openApplication(page: Page, appName: string): Promise<void> {
    await page.getByRole('button', {name: appName}).click();
    await expect(page.getByRole('heading', {name: appName})).toBeVisible();
}
