import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';
import {cancelCheckout} from '../../utils/stripe-checkout.util';

/**
 * Sandbox tests for checkout cancellation flow.
 * Verifies that canceling checkout returns to cart with items preserved.
 */
test.describe('Stripe Sandbox - Checkout Cancellation', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let appName: string;
    const envName = 'Cancel Test Environment';

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        appName = setup.appName;
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('setup: create paid environment', async () => {
        // Navigate to the application
        await sharedPage.goto('/dashboard');
        await sharedPage.getByRole('button', {name: appName}).click();
        await sharedPage.waitForLoadState('networkidle');

        // Create a PRO tier environment (paid)
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        await sharedPage.locator('#envName').fill(envName);
        await sharedPage.locator('#envDesc').fill('Testing checkout cancellation');

        // Select PRO tier
        const proTier = sharedPage.locator('.tier-option').filter({hasText: 'Pro'});
        await proTier.click();

        await sharedPage.locator('.environment-creation-overlay')
            .getByRole('button', {name: /Create Environment/}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();
    });

    test('can navigate to checkout with pending environment', async () => {
        await sharedPage.goto('/dashboard/checkout');

        const cartItem = sharedPage.locator('.cart-item').filter({hasText: envName});
        await expect(cartItem).toBeVisible();
        await expect(cartItem).toContainText('PRO');
    });

    test('can cancel checkout and return to cart', async () => {
        // Use the cancelCheckout utility
        await cancelCheckout(sharedPage);

        // Should be back on checkout page (cancel URL)
        await expect(sharedPage).toHaveURL(/checkout$/);
    });

    test('cart still shows pending environment after cancel', async () => {
        // The environment should still be in the cart
        const cartItem = sharedPage.locator('.cart-item').filter({hasText: envName});
        await expect(cartItem).toBeVisible();
        await expect(cartItem).toContainText('PRO');
        await expect(cartItem).toContainText('$50');
    });

    test('proceed to payment button still available', async () => {
        const checkoutButton = sharedPage.getByRole('button', {name: 'Proceed to Payment'});
        await expect(checkoutButton).toBeVisible();
        await expect(checkoutButton).toBeEnabled();
    });

    test('environment remains in PENDING status', async () => {
        // Navigate to dashboard to verify cart badge
        await sharedPage.goto('/dashboard');

        // Cart badge should still show 1 item
        const cartBadge = sharedPage.locator('.cart-badge');
        await expect(cartBadge).toBeVisible();
        await expect(cartBadge).toHaveText('1');
    });
});
