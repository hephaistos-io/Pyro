import {expect, Page, test} from '@playwright/test';
import {
    addUserField,
    getReadApiKey,
    getWithApiKey,
    getWriteApiKey,
    navigateToTemplateTab,
    postWithApiKey,
    selectUserTemplateType,
    setupUserWithApplication
} from '../../utils';

test.describe('User Statistics', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let writeApiKey: string;
    let readApiKey: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add USER template fields (required for user endpoint)
        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
        await addUserField(sharedPage, 'theme', 'STRING');

        // Get API keys
        writeApiKey = await getWriteApiKey(sharedPage);
        readApiKey = await getReadApiKey(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('initially shows zero users', async () => {
        // Navigate to overview
        await sharedPage.getByRole('button', {name: 'Overview', exact: true}).click();

        // Wait for statistics to load
        await sharedPage.waitForTimeout(500);

        // Verify Total Users shows 0
        const totalUsersValue = sharedPage.locator('.summary-stat')
            .filter({hasText: 'Total Users'})
            .locator('.summary-stat__value');
        await expect(totalUsersValue).toHaveText('0');
    });

    test('displays correct user count after creating users via API', async () => {
        // Create users via customer-api POST endpoint
        const result1 = await postWithApiKey(
            sharedPage,
            '/v1/api/templates/user/user1',
            writeApiKey,
            {theme: 'dark'}
        );
        expect(result1.status).toBe(200);

        const result2 = await postWithApiKey(
            sharedPage,
            '/v1/api/templates/user/user2',
            writeApiKey,
            {theme: 'light'}
        );
        expect(result2.status).toBe(200);

        const result3 = await postWithApiKey(
            sharedPage,
            '/v1/api/templates/user/user3',
            writeApiKey,
            {theme: 'blue'}
        );
        expect(result3.status).toBe(200);

        // Navigate to dashboard and back to app to reload statistics
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.locator('.app-card:not(.app-card--add)')).toBeVisible();

        // Click on the application to go back
        await sharedPage.locator('.app-card:not(.app-card--add)').first().click();

        // Wait for page to load and statistics to update
        await sharedPage.waitForTimeout(1000);

        // Verify Total Users shows 3
        const totalUsersValue = sharedPage.locator('.summary-stat')
            .filter({hasText: 'Total Users'})
            .locator('.summary-stat__value');
        await expect(totalUsersValue).toHaveText('3');
    });

    test('counts unique users only (same user id updates, not creates)', async () => {
        // Update an existing user (should not increase count)
        const result = await postWithApiKey(
            sharedPage,
            '/v1/api/templates/user/user1',
            writeApiKey,
            {theme: 'updated-theme'}
        );
        expect(result.status).toBe(200);

        // Navigate to dashboard and back to reload statistics
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.locator('.app-card:not(.app-card--add)')).toBeVisible();
        await sharedPage.locator('.app-card:not(.app-card--add)').first().click();

        await sharedPage.waitForTimeout(1000);

        // Verify Total Users still shows 3 (not 4)
        const totalUsersValue = sharedPage.locator('.summary-stat')
            .filter({hasText: 'Total Users'})
            .locator('.summary-stat__value');
        await expect(totalUsersValue).toHaveText('3');
    });

    test('tracks hits this month after API requests', async () => {
        // Make several GET requests to customer-api to generate hits
        for (let i = 0; i < 5; i++) {
            const result = await getWithApiKey(sharedPage, '/v1/api/templates/system', readApiKey);
            expect(result.status).toBe(200);
        }

        // Navigate to dashboard and back to reload statistics
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.locator('.app-card:not(.app-card--add)')).toBeVisible();
        await sharedPage.locator('.app-card:not(.app-card--add)').first().click();

        await sharedPage.waitForTimeout(1000);

        // Verify Hits This Month shows at least 5
        // (Previous tests may have added some hits too)
        const hitsValue = sharedPage.locator('.summary-stat')
            .filter({hasText: 'Hits This Month'})
            .locator('.summary-stat__value');
        const hitsText = await hitsValue.textContent();
        const hits = parseInt(hitsText?.replace(/,/g, '') ?? '0', 10);
        expect(hits).toBeGreaterThanOrEqual(5);
    });
});
