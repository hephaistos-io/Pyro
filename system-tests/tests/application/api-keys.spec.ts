import {expect, Page, test} from '@playwright/test';

/**
 * Helper to generate unique test data
 */
function uniqueEmail(): string {
    return `apikey-test-${Date.now()}-${crypto.randomUUID()}@example.com`;
}

/**
 * Helper to set up a complete user with company and application
 */
async function setupUserWithApplication(page: Page): Promise<{ email: string; appName: string }> {
    const email = uniqueEmail();
    const password = 'SecurePassword123!@#';
    const companyName = `TestCo-${Date.now()}`;
    const appName = `TestApp-${Date.now()}`;

    // Register
    await page.goto('/register');
    await page.getByLabel('First Name').fill('Test');
    await page.getByLabel('Last Name').fill('User');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', {exact: true}).fill(password);
    await page.getByLabel('Confirm Password').fill(password);
    await page.getByRole('button', {name: 'Create Account'}).click();

    // Wait for redirect to login
    await expect(page).toHaveURL('/login', {timeout: 15000});

    // Login
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', {name: 'Log In'}).click();

    // Wait for dashboard
    await expect(page).toHaveURL('/dashboard', {timeout: 15000});

    // Create company (onboarding overlay should appear)
    await expect(page.getByText('Create Your Company')).toBeVisible({timeout: 10000});
    await page.getByLabel('Company Name').fill(companyName);
    await page.getByRole('button', {name: 'Create Company'}).click();

    // Wait for company to be created and overlay to close
    await expect(page.getByText('Create Your Company')).not.toBeVisible({timeout: 10000});

    // Create application
    await page.getByRole('button', {name: 'New Application'}).click();
    await expect(page.getByText('Create New Application')).toBeVisible({timeout: 5000});
    await page.getByLabel('Application Name').fill(appName);
    await page.getByRole('button', {name: 'Create Application'}).click();

    // Wait for application to be created
    await expect(page.getByText('Create New Application')).not.toBeVisible({timeout: 10000});

    // Click on the application to navigate to overview
    await page.getByRole('button', {name: appName}).click();

    // Wait for application overview page
    await expect(page.getByRole('heading', {name: appName})).toBeVisible({timeout: 10000});

    return {email, appName};
}

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

        // Wait for loading to complete and key to appear
        await expect(readKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});

        // The key should now be visible (64 hex characters)
        const keyCode = readKeySection.locator('.stat__code--key');
        const keyText = await keyCode.textContent();

        // Verify it's an actual key (64 hex chars), not placeholder
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

        // Wait for loading to complete
        await expect(writeKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});

        // The key should now be visible (64 hex characters)
        const keyCode = writeKeySection.locator('.stat__code--key');
        const keyText = await keyCode.textContent();

        // Verify it's an actual key (64 hex chars), not placeholder
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

    test('clicking refresh button when key is hidden shows confirmation modal', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const refreshButton = readKeySection.getByRole('button', {name: 'Refresh key'});

        // Click refresh (key is hidden at this point)
        await refreshButton.click();

        // Confirmation modal should appear
        await expect(sharedPage.getByText('Refresh Read')).toBeVisible();
        await expect(sharedPage.getByText('This action will immediately invalidate the current key')).toBeVisible();

        // Cancel to close modal
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();
        await expect(sharedPage.getByText('Refresh Read')).not.toBeVisible();
    });

    test('show Read-Key before regeneration tests', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});

        // Show the key for the next tests
        await readKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(readKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});
        await expect(readKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();
    });

    test('clicking refresh button shows confirmation modal', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});
        const refreshButton = readKeySection.getByRole('button', {name: 'Refresh key'});

        // Click refresh
        await refreshButton.click();

        // Confirmation modal should appear
        await expect(sharedPage.getByText('Refresh Read')).toBeVisible();
        await expect(sharedPage.getByText('This action will immediately invalidate the current key')).toBeVisible();

        // Cancel button should be visible
        await expect(sharedPage.getByRole('button', {name: 'Cancel'})).toBeVisible();

        // Confirm button should be visible
        await expect(sharedPage.getByRole('button', {name: 'I understand'})).toBeVisible();
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
        // Wait for loading to complete
        await expect(readKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});
        // Wait for key to appear (matches 64 hex chars)
        await expect(readKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i, {timeout: 10000});

        // Get the current key value
        const keyBefore = await readKeySection.locator('.stat__code--key').textContent();
        expect(keyBefore?.trim()).toMatch(/^[a-f0-9]{64}$/i); // Ensure key is actually shown

        // Open refresh modal
        await readKeySection.getByRole('button', {name: 'Refresh key'}).click();
        await expect(sharedPage.getByText('Refresh Read')).toBeVisible();

        // Confirm refresh
        await sharedPage.getByRole('button', {name: 'I understand'}).click();

        // Wait for modal to close and key to be updated
        await expect(sharedPage.getByText('Refresh Read')).not.toBeVisible({timeout: 10000});

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
        await expect(writeKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});

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
        // Wait for loading to complete
        await expect(writeKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});
        // Wait for key to appear (matches 64 hex chars)
        await expect(writeKeySection.locator('.stat__code--key')).toContainText(/[a-f0-9]{64}/i, {timeout: 10000});

        // Get the current key value
        const keyBefore = await writeKeySection.locator('.stat__code--key').textContent();
        expect(keyBefore?.trim()).toMatch(/^[a-f0-9]{64}$/i); // Ensure key is actually shown

        // Open refresh modal
        await writeKeySection.getByRole('button', {name: 'Refresh key'}).click();
        await expect(sharedPage.getByText('Refresh Write')).toBeVisible();

        // Confirm refresh
        await sharedPage.getByRole('button', {name: 'I understand'}).click();

        // Wait for modal to close and key to be updated
        await expect(sharedPage.getByText('Refresh Write')).not.toBeVisible({timeout: 10000});

        // Key should be different
        const keyAfter = await writeKeySection.locator('.stat__code--key').textContent();
        expect(keyAfter?.trim()).toMatch(/^[a-f0-9]{64}$/i);
        expect(keyAfter).not.toBe(keyBefore);
    });

    test('regenerated key persists after page refresh', async () => {
        const readKeySection = sharedPage.locator('.stat').filter({hasText: 'Read-Key'});

        // Get current key value
        const keyBeforeRefresh = await readKeySection.locator('.stat__code--key').textContent();

        // Reload the page
        await sharedPage.reload();

        // Wait for page to load
        await expect(sharedPage.getByRole('heading', {name: sharedAppName})).toBeVisible({timeout: 10000});

        // Show the Read-Key again
        await readKeySection.getByRole('button', {name: 'Show key'}).click();
        await expect(readKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});

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
        await expect(readKeySection.getByText('Loading...')).not.toBeVisible({timeout: 10000});

        // Verify key is shown
        await expect(readKeySection.getByRole('button', {name: 'Hide key'})).toBeVisible();

        // Create a new environment
        await page.locator('.selector-button').click();
        await expect(page.getByText('Create Environment')).toBeVisible();
        await page.getByRole('button', {name: 'Create Environment'}).click();

        // Fill in environment form
        await expect(page.getByText('Add New Environment')).toBeVisible();
        await page.getByLabel('Environment Name').fill(`Staging-${Date.now()}`);
        await page.getByLabel('Description').fill('Test environment');
        await page.getByRole('button', {name: 'Create Environment'}).click();

        // Wait for environment to be created
        await expect(page.getByText('Add New Environment')).not.toBeVisible({timeout: 10000});

        // The key should now be hidden (state cleared on environment change)
        await expect(readKeySection.locator('.stat__code--key')).toContainText('••••••••');
        await expect(readKeySection.getByRole('button', {name: 'Show key'})).toBeVisible();
    });
});
