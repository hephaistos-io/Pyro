import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Checkout Page', () => {
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

    test('checkout page with empty cart shows empty state', async () => {
        await sharedPage.goto('/dashboard/checkout');

        // Should show empty cart message
        await expect(sharedPage.locator('.empty-cart')).toBeVisible();

        // Checkout button should be disabled
        const checkoutButton = sharedPage.getByRole('button', {name: 'Proceed to Payment'});
        await expect(checkoutButton).toBeDisabled();
    });

    test('can add paid environment and navigate to checkout', async () => {
        // Go back to the application
        await sharedPage.goto('/dashboard');
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Wait for app to load
        await sharedPage.waitForTimeout(500);

        // Create paid environment
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        await sharedPage.locator('#envName').fill('Premium Environment');
        await sharedPage.locator('#envDesc').fill('For checkout testing');

        // Select PRO tier
        const proTierButton = sharedPage.locator('.tier-option').filter({hasText: 'Pro'});
        await proTierButton.click();

        // Create the environment
        await sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: /Create Environment/}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();

        // Wait for UI to update
        await sharedPage.waitForTimeout(1000);

        // Navigate to checkout
        await sharedPage.goto('/dashboard/checkout');
    });

    test('checkout page shows pending environment', async () => {
        // Should show the cart item
        const cartItem = sharedPage.locator('.cart-item').filter({hasText: 'Premium Environment'});
        await expect(cartItem).toBeVisible();

        // Should show the tier
        await expect(cartItem.locator('.item-tier')).toContainText('PRO');

        // Should show the price
        await expect(cartItem).toContainText('$50');
    });

    test('checkout page shows correct total', async () => {
        const total = sharedPage.locator('.cart-total');
        await expect(total).toBeVisible();
        await expect(total).toContainText('$50');
    });

    test('checkout page has proceed to payment button', async () => {
        const checkoutButton = sharedPage.getByRole('button', {name: 'Proceed to Payment'});
        await expect(checkoutButton).toBeVisible();
        await expect(checkoutButton).toBeEnabled();
    });

    test('checkout page has back to dashboard link', async () => {
        const backLink = sharedPage.locator('.back-link');
        await expect(backLink).toBeVisible();
        await expect(backLink).toContainText('Back to Dashboard');
    });

    test('can navigate to billing settings from checkout', async () => {
        const billingLink = sharedPage.getByRole('link', {name: 'View Billing'});
        if (await billingLink.isVisible()) {
            await billingLink.click();
            await expect(sharedPage).toHaveURL(/\/dashboard\/billing/);
        }
    });
});
