import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Pricing Statistics', () => {
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

    test('displays pricing statistics on dashboard for admin users', async () => {
        // Navigate back to dashboard
        await sharedPage.goto('/dashboard');

        // Verify pricing statistics section is visible
        await expect(sharedPage.getByRole('heading', {name: 'Monthly Cost Breakdown'})).toBeVisible();
    });

    test('shows Free tier for first application', async () => {
        await sharedPage.goto('/dashboard');

        // Find the costs chart row for the application
        const appRow = sharedPage.locator('.costs-chart__row').first();

        // Verify the application name is displayed
        await expect(appRow.locator('.costs-chart__name')).toContainText(sharedAppName);

        // Verify "Free tier included" detail is shown
        await expect(appRow.locator('.costs-chart__detail--free')).toContainText('Free tier included');

        // Verify amount is "Free"
        await expect(appRow.locator('.costs-chart__amount')).toContainText('Free');
    });

    test('shows total monthly price as Free for single free tier application', async () => {
        await sharedPage.goto('/dashboard');

        // Verify the total is displayed as "Free"
        const totalElement = sharedPage.locator('.costs-overview__total');
        await expect(totalElement).toContainText('Free');
    });

    test('displays pricing legend with free and paid tier indicators', async () => {
        await sharedPage.goto('/dashboard');

        // Verify legend items exist
        await expect(sharedPage.locator('.costs-legend__text').filter({hasText: 'Free tier environments'})).toBeVisible();
        await expect(sharedPage.locator('.costs-legend__text').filter({hasText: 'Paid tier environments'})).toBeVisible();

        // Verify legend dots have correct styling
        await expect(sharedPage.locator('.costs-legend__dot--free')).toBeVisible();
        await expect(sharedPage.locator('.costs-legend__dot--paid')).toBeVisible();
    });

    test('pricing statistics shows environment count in detail', async () => {
        await sharedPage.goto('/dashboard');

        const appRow = sharedPage.locator('.costs-chart__row').first();
        const detail = appRow.locator('.costs-chart__detail');

        // Should show either "Free tier included" or environment count
        const detailText = await detail.textContent();
        expect(detailText).toMatch(/Free tier included|environment/);
    });
});
