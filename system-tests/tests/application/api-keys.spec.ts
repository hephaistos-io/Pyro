import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication, uniqueName} from '../../utils';

test.describe('API Key Management', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let sharedAppName: string;

    test.beforeAll(async ({browser}) => {
        // Create a single browser context for all tests in this suite
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        sharedAppName = setup.appName;
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays API keys section with Read-Key and Write-Key', async () => {
        // Verify API Attributes section exists
        await expect(sharedPage.getByRole('heading', {name: 'API Attributes'})).toBeVisible();

        // Verify Read-Key and Write-Key labels exist
        await expect(sharedPage.getByText('Read-Key')).toBeVisible();
        await expect(sharedPage.getByText('Write-Key')).toBeVisible();
    });

    test('shows placeholder dots when keys are hidden by default', async () => {
        // The placeholder should be visible (dots pattern)
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});

        // Both keys should show placeholder (dots)
        await expect(readKeySection.locator('.stat__code--key')).toContainText('••••••••');
        await expect(writeKeySection.locator('.stat__code--key')).toContainText('••••••••');
    });

    test('shows "Show key" buttons for both keys', async () => {
        // Both show buttons should be visible with aria-label "Show key"
        const showButtons = sharedPage.getByRole('button', {name: 'Show key'});
        await expect(showButtons).toHaveCount(2);
    });

    test('clicking show button reveals the Read-Key from backend', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const showButton = readKeySection.getByRole('button', {name: 'Show key'});

        // Click show button
        await showButton.click();

        // Wait for the actual key to appear (64 hex characters)
        const keyCode = readKeySection.locator('.stat__code--key');
        await expect(keyCode).toContainText(/[a-f0-9]{64}/i);

        // Verify it's an actual key (64 hex chars), not placeholder
        const keyText = await keyCode.textContent();
        expect(keyText?.trim()).toMatch(/^[a-f0-9]{64}$/i);

        // Button should now say "Hide key"
        await expect(readKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();
    });

    test('clicking hide button hides the Read-Key and shows placeholder', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const hideButton = readKeySection.getByRole('button', {name: 'Hide key'});

        // Click hide button
        await hideButton.click();

        // The placeholder should be visible again
        await expect(readKeySection.locator('.stat__code--key')).toContainText('••••••••');

        // Button should now say "Show key" again
        await expect(readKeySection.getByRole('button', {name: 'Show key'})).toBeVisible();
    });

    test('clicking show button reveals the Write-Key from backend', async () => {
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});
        const showButton = writeKeySection.getByRole('button', {name: 'Show key'});

        // Click show button
        await showButton.click();

        // Wait for the actual key to appear (64 hex characters)
        const keyCode = writeKeySection.locator('.stat__code--key');
        await expect(keyCode).toContainText(/[a-f0-9]{64}/i);

        // Verify it's an actual key (64 hex chars), not placeholder
        const keyText = await keyCode.textContent();
        expect(keyText?.trim()).toMatch(/^[a-f0-9]{64}$/i);

        // Button should now say "Hide key"
        await expect(writeKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();
    });

    test('clicking hide button hides the Write-Key and shows placeholder', async () => {
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});
        const hideButton = writeKeySection.getByRole('button', {name: 'Hide key'});

        // Click hide button
        await hideButton.click();

        // The placeholder should be visible again
        await expect(writeKeySection.locator('.stat__code--key')).toContainText('••••••••');

        // Button should now say "Show key" again
        await expect(writeKeySection.getByRole('button', {name: 'Show key'})).toBeVisible();
    });

    test('refresh button is always enabled for both keys', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});

        // Both refresh buttons should be enabled (regardless of whether key is shown)
        const readRefreshButton = readKeySection.getByRole('button', {name: 'Refresh key'});
        const writeRefreshButton = writeKeySection.getByRole('button', {name: 'Refresh key'});

        await expect(readRefreshButton).toBeEnabled();
        await expect(writeRefreshButton).toBeEnabled();
    });

    test('show Read-Key before regeneration tests', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});

        // Show the key for the next tests
        await readKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);
        await expect(readKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();
    });

    test('clicking refresh button shows confirmation modal', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const refreshButton = readKeySection.getByRole('button', {name: 'Refresh Key'});

        // Click refresh
        await refreshButton.click();

        // Confirmation modal should appear
        await expect(sharedPage.getByText('Refresh Read')).toBeVisible();
        await expect(sharedPage.getByText('A new Read-Key will be generated.')).toBeVisible();

        // Cancel button should be visible
        await expect(sharedPage.getByRole('button', {name: 'Cancel'})).toBeVisible();

        // Confirm button should be visible
        await expect(sharedPage.getByRole('button', {name: 'Generate New Key'})).toBeVisible();
    });

    test('canceling refresh modal closes it without changing key', async () => {
        // Get the current key value
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const keyBefore = await readKeySection.locator('.stat__code--key').textContent();

        // Cancel the modal
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();

        // Modal should close
        await expect(sharedPage.getByText('Refresh Read')).not.toBeVisible();

        // Key should be unchanged
        const keyAfter = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyAfter).toBe(keyBefore);
    });

    test('confirming refresh regenerates the key', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});

        // First ensure the key is shown (makes test self-contained)
        const showButton = readKeySection.getByRole('button', {name: 'Show key'});
        if (await showButton.isVisible()) {
            await showButton.click();
        }
        // Wait for key to appear (matches 64 hex chars)
        await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);

        // Get the current key value
        const keyBefore = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyBefore?.trim()).toMatch(/^[a-f0-9]{64}$/i); // Ensure key is actually shown

        // Open refresh modal
        await readKeySection.getByRole('button', {name: 'Refresh key'}).click();
        await expect(sharedPage.getByText('Refresh Read')).toBeVisible();

        // Confirm refresh
        await sharedPage.getByRole('button', {name: 'Generate New Key'}).click();

        // Wait for success state and close the modal
        await expect(sharedPage.getByText('Key Generated Successfully')).toBeVisible();
        await sharedPage.getByRole('button', {name: 'Close', exact: true}).filter({hasText: 'Close'}).click();

        // Wait for modal to close
        await expect(sharedPage.getByText('Key Generated Successfully')).not.toBeVisible();

        // Key should be different
        const keyAfter = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyAfter?.trim()).toMatch(/^[a-f0-9]{64}$/i);
        expect(keyAfter).not.toBe(keyBefore);
    });

    test('Read-Key and Write-Key are different values', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});

        // Show both keys
        // Read key should already be shown from previous test
        // Show write key
        await writeKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(writeKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);

        // Get both key values
        const readKey = await readKeySection.locator('.stat__code--key').textContent();
        const writeKey = await writeKeySection.locator('.stat__code--key').textContent();

        // Keys should be different
        expect(readKey?.trim()).not.toBe(writeKey?.trim());
    });

    test('clicking refresh on Write-Key shows correct modal', async () => {
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});
        const refreshButton = writeKeySection.getByRole('button', {name: 'Refresh key'});

        // Click refresh
        await refreshButton.click();

        // Confirmation modal should show "Write-Key" in the title
        await expect(sharedPage.getByText('Refresh Write')).toBeVisible();

        // Cancel to close modal
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();
        await expect(sharedPage.getByText('Refresh Write')).not.toBeVisible();
    });

    test('confirming refresh on Write-Key regenerates the key', async () => {
        const writeKeySection = sharedPage.locator('.stat').filter({hasText: 'Write-Key'});

        // First ensure the key is shown (makes test self-contained)
        const showButton = writeKeySection.getByRole('button', {name: 'Show key'});
        if (await showButton.isVisible()) {
            await showButton.click();
        }
        // Wait for key to appear (matches 64 hex chars)
        await expect(writeKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);

        // Get the current key value
        const keyBefore = await writeKeySection.locator('.stat__code--key').textContent();
        expect(keyBefore?.trim()).toMatch(/^[a-f0-9]{64}$/i); // Ensure key is actually shown

        // Open refresh modal
        await writeKeySection.getByRole('button', {name: 'Refresh key'}).click();
        await expect(sharedPage.getByText('Refresh Write')).toBeVisible();

        // Confirm refresh
        await sharedPage.getByRole('button', {name: 'Generate New Key'}).click();

        // Wait for success state and close the modal
        await expect(sharedPage.getByText('Key Generated Successfully')).toBeVisible();
        await sharedPage.getByRole('button', {name: 'Close', exact: true}).filter({hasText: 'Close'}).click();

        // Wait for modal to close
        await expect(sharedPage.getByText('Key Generated Successfully')).not.toBeVisible();

        // Key should be different
        const keyAfter = await writeKeySection.locator('.stat__code--key').textContent();
        expect(keyAfter?.trim()).toMatch(/^[a-f0-9]{64}$/i);
        expect(keyAfter).not.toBe(keyBefore);
    });

    test('regenerated key persists after page refresh', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});

        // Ensure key is visible first
        const showButton = readKeySection.getByRole('button', {name: 'Show key'});
        if (await showButton.isVisible()) {
            await showButton.click();
            await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);
        }

        // Get current key value (should be an actual key, not placeholder)
        const keyBeforeRefresh = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyBeforeRefresh?.trim()).toMatch(/^[a-f0-9]{64}$/i);

        // Navigate to dashboard and back to app (more reliable across browsers)
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Wait for app page to load
        await expect(sharedPage.getByRole('heading', {name: sharedAppName})).toBeVisible();

        // Show the Read-Key again
        await readKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);

        // Key should be the same as before page refresh
        const keyAfterRefresh = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyAfterRefresh?.trim()).toBe(keyBeforeRefresh?.trim());
    });
});

test.describe('API Key State Isolation', () => {
    test('keys are cleared when switching environments', async ({page}) => {
        // This test requires creating multiple environments
        // Set up user with application first
        await setupUserWithApplication(page);

        const readKeySection = page.locator('.stat').filter({hasText: 'Read-Key'});

        // Show the read key
        await readKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i);

        // Verify key is shown
        await expect(readKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();

        // Create a new environment
        await page.locator('.selector-button').click();
        await expect(page.getByText('Create Environment')).toBeVisible();
        await page.getByRole('button', {name: 'Create Environment'}).click();

        // Fill in environment form
        await expect(page.getByText('Add New Environment')).toBeVisible();
        await page.getByLabel('Environment Name').fill(uniqueName('Staging'));
        await page.getByLabel('Description').fill('Test environment');
        await page.getByRole('button', {name: 'Create Environment'}).click();

        // Wait for environment to be created
        await expect(page.getByText('Add New Environment')).not.toBeVisible();

        // The key should now be hidden (state cleared on environment change)
        await expect(readKeySection.locator('.stat__code--key')).toContainText('••••••••');
        await expect(readKeySection.getByRole('button', {name: 'Show key'})).toBeVisible();
    });
});
