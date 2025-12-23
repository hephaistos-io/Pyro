import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Environment Management', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let sharedAppName: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        sharedAppName = setup.appName;
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays a default environment', async () => {
        const selector = sharedPage.locator('.selector-button');
        await expect(selector).toBeVisible();
        // Should display either Production or Development (whichever is selected first)
        const text = await selector.textContent();
        expect(text?.trim()).toMatch(/Production|Development/);
    });

    test('can open environment dropdown', async () => {
        await sharedPage.locator('.selector-button').click();
        await expect(sharedPage.locator('.dropdown-menu')).toBeVisible();
    });

    test('shows both default environments in dropdown', async () => {
        // Dropdown should already be open from previous test
        const productionItem = sharedPage.locator('.dropdown-item').filter({hasText: 'Production'});
        const developmentItem = sharedPage.locator('.dropdown-item').filter({hasText: 'Development'});

        await expect(productionItem).toBeVisible();
        await expect(developmentItem).toBeVisible();
    });

    test('can create a new environment', async () => {
        // Close dropdown if it's open from previous test
        const dropdownOverlay = sharedPage.locator('.dropdown-overlay');
        if (await dropdownOverlay.isVisible()) {
            await dropdownOverlay.click();
        }

        // Click Create Environment button (standalone button next to selector)
        await sharedPage.locator('.add-env-btn').click();

        // Wait for overlay to appear
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();
        await expect(sharedPage.getByText('Add New Environment')).toBeVisible();

        // Fill in environment details
        await sharedPage.locator('#envName').fill('Staging');
        await sharedPage.locator('#envDesc').fill('Staging environment for QA');

        // Click Create button (scope to overlay to avoid matching the add-env-btn)
        await sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: 'Create Environment'}).click();

        // Wait for overlay to close
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();

        // Wait for environment to be created
        await sharedPage.waitForTimeout(1000);

        // Verify we switched to the new environment
        await expect(sharedPage.locator('.selector-button')).toContainText('Staging');
    });

    test('cannot delete FREE tier environment (Production)', async () => {
        // Check current environment
        const currentEnv = await sharedPage.locator('.selector-button').textContent();

        // If not on Production, switch to it
        if (!currentEnv?.includes('Production')) {
            await sharedPage.locator('.selector-button').click();
            const productionOption = sharedPage.locator('.dropdown-item').filter({hasText: 'Production'});
            await expect(productionOption).toBeVisible();
            await productionOption.click();

            // Wait for environment to switch
            await sharedPage.waitForTimeout(500);
        }

        // Delete button should not be visible for FREE tier
        await expect(sharedPage.locator('.delete-env-btn')).not.toBeVisible();
    });

    test('can delete paid environment (Staging)', async () => {
        // Switch to Staging environment
        await sharedPage.locator('.selector-button').click();
        const stagingOption = sharedPage.locator('.dropdown-item').filter({hasText: 'Staging'});
        await expect(stagingOption).toBeVisible();
        await stagingOption.click();

        // Wait for environment to switch
        await sharedPage.waitForTimeout(500);

        // Delete button should be visible for paid tier
        await expect(sharedPage.locator('.delete-env-btn')).toBeVisible();
    });

    test('delete environment shows confirmation overlay', async () => {
        // Click delete button
        await sharedPage.locator('.delete-env-btn').click();

        // Verify overlay appears
        await expect(sharedPage.locator('.delete-field-overlay')).toBeVisible();
        await expect(sharedPage.getByRole('heading', {name: 'Delete Environment "Staging"?'})).toBeVisible();
    });

    test('delete overlay requires application name', async () => {
        // Get the application name from the hint
        const appNameHint = sharedPage.locator('.confirm-group').filter({hasText: 'Application Name'}).locator('.confirm-hint code');
        const appName = await appNameHint.textContent();
        expect(appName).toBe(sharedAppName);

        // Enter environment name but not application name
        const envNameInput = sharedPage.locator('input[placeholder="Enter environment name"]');
        await envNameInput.fill('Staging');

        // Delete button should still be disabled (scope to overlay)
        const deleteButton = sharedPage.locator('.delete-field-overlay').getByRole('button', {name: 'Delete Environment'});
        await expect(deleteButton).toBeDisabled();

        // Clear the environment name input
        await envNameInput.clear();
    });

    test('delete overlay requires environment name', async () => {
        // Enter application name but not environment name
        const appNameInput = sharedPage.locator('input[placeholder="Enter application name"]');
        await appNameInput.fill(sharedAppName);

        // Delete button should still be disabled (scope to overlay)
        const deleteButton = sharedPage.locator('.delete-field-overlay').getByRole('button', {name: 'Delete Environment'});
        await expect(deleteButton).toBeDisabled();
    });

    test('delete overlay shows visual feedback for correct inputs', async () => {
        // Enter environment name (application name should already be filled from previous test)
        const envNameInput = sharedPage.locator('input[placeholder="Enter environment name"]');
        await envNameInput.fill('Staging');

        // Should have at least one check mark (visual feedback working)
        const checkMarks = sharedPage.locator('.confirm-check');
        expect(await checkMarks.count()).toBeGreaterThanOrEqual(1);
    });

    test('delete button is enabled when both fields match', async () => {
        const deleteButton = sharedPage.locator('.delete-field-overlay').getByRole('button', {name: 'Delete Environment'});
        await expect(deleteButton).toBeEnabled();
    });

    test('can cancel environment deletion', async () => {
        const cancelButton = sharedPage.getByRole('button', {name: 'Cancel'});
        await cancelButton.click();

        // Overlay should close
        await expect(sharedPage.locator('.delete-field-overlay')).not.toBeVisible();

        // Environment should still exist
        await expect(sharedPage.locator('.selector-button')).toContainText('Staging');
    });

    test('can confirm environment deletion', async () => {
        // Click delete button again
        await sharedPage.locator('.delete-env-btn').click();

        // Wait for overlay
        await expect(sharedPage.locator('.delete-field-overlay')).toBeVisible();

        // Fill in both fields
        const appNameInput = sharedPage.locator('input[placeholder="Enter application name"]');
        const envNameInput = sharedPage.locator('input[placeholder="Enter environment name"]');

        await appNameInput.fill(sharedAppName);
        await envNameInput.fill('Staging');

        // Click delete (scope to overlay)
        const deleteButton = sharedPage.locator('.delete-field-overlay').getByRole('button', {name: 'Delete Environment'});
        await expect(deleteButton).toBeEnabled();
        await deleteButton.click();

        // Wait for overlay to close
        await expect(sharedPage.locator('.delete-field-overlay')).not.toBeVisible();

        // Wait for deletion to complete
        await sharedPage.waitForTimeout(1000);

        // Should switch to another environment (Development or Production)
        const selectorText = await sharedPage.locator('.selector-button').textContent();
        expect(selectorText).not.toContain('Staging');
    });

    test('deleted environment is removed from dropdown', async () => {
        // Open dropdown
        await sharedPage.locator('.selector-button').click();
        await expect(sharedPage.locator('.dropdown-menu')).toBeVisible();

        // Staging should not be in the list
        const stagingItem = sharedPage.locator('.dropdown-item').filter({hasText: 'Staging'});
        await expect(stagingItem).not.toBeVisible();

        // But Production and Development should still be there
        await expect(sharedPage.locator('.dropdown-item').filter({hasText: 'Production'})).toBeVisible();
        await expect(sharedPage.locator('.dropdown-item').filter({hasText: 'Development'})).toBeVisible();

        // Close dropdown
        await sharedPage.locator('.dropdown-overlay').click();
    });

    test('deleted environment stays deleted after page refresh', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Wait for page to load
        await sharedPage.waitForTimeout(1000);

        // Open dropdown
        await sharedPage.locator('.selector-button').click();
        await expect(sharedPage.locator('.dropdown-menu')).toBeVisible();

        // Staging should still not be in the list
        const stagingItem = sharedPage.locator('.dropdown-item').filter({hasText: 'Staging'});
        await expect(stagingItem).not.toBeVisible();
    });
});
