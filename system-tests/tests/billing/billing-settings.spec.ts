import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Billing Tab', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('can navigate to billing tab from dashboard', async () => {
        // Navigate to billing tab
        await sharedPage.goto('/dashboard/billing');

        // Should show billing section title
        await expect(sharedPage.locator('.section-title')).toContainText('Billing & Subscription');

        // Billing tab should be active
        const billingTab = sharedPage.getByRole('button', {name: 'Billing', exact: true});
        await expect(billingTab).toHaveClass(/dashboard-tab--active/);
    });

    test('can click billing tab to switch to billing view', async () => {
        // Start at main dashboard
        await sharedPage.goto('/dashboard');

        // Click the Billing tab
        const billingTab = sharedPage.getByRole('button', {name: 'Billing', exact: true});
        await billingTab.click();

        // Should show billing content
        await expect(sharedPage.locator('.section-title')).toContainText('Billing & Subscription');
        await expect(sharedPage).toHaveURL(/\/dashboard\/billing/);
    });

    test('shows subscription section', async () => {
        // Should have a subscription card
        const subscriptionCard = sharedPage.locator('.billing-card').first();
        await expect(subscriptionCard).toBeVisible();

        // Should show subscription header
        await expect(sharedPage.getByRole('heading', {name: 'Subscription', exact: true})).toBeVisible();
    });

    test('shows manage billing button', async () => {
        const manageBillingButton = sharedPage.getByRole('button', {name: 'Manage Billing'});
        await expect(manageBillingButton).toBeVisible();
    });

    test('shows no subscription message when user has no subscription', async () => {
        // New user should see no subscription message
        const noSubMessage = sharedPage.locator('.no-subscription');
        await expect(noSubMessage).toBeVisible();
        await expect(noSubMessage).toContainText("don't have an active subscription");
    });

    test('shows invoice history section', async () => {
        await expect(sharedPage.getByRole('heading', {name: 'Invoice History'})).toBeVisible();
    });

    test('shows no invoices message when user has no invoices', async () => {
        const noInvoicesMessage = sharedPage.locator('.no-invoices');
        await expect(noInvoicesMessage).toBeVisible();
        await expect(noInvoicesMessage).toContainText('No invoices yet');
    });

    test('can switch back to applications tab', async () => {
        // Click the Applications tab
        const applicationsTab = sharedPage.getByRole('button', {name: 'Applications'});
        await applicationsTab.click();

        // Should show applications content and update URL
        await expect(sharedPage.locator('.section-title')).toContainText('Applications of');
        await expect(sharedPage).toHaveURL(/\/dashboard$/);
    });
});
