import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';
import {TEST_CARDS, waitForWebhookProcessing, waitForCartEmpty, fillStripeCardDetails} from '../../utils/stripe-checkout.util';

/**
 * Sandbox tests for successful Stripe checkout flow.
 * Uses real Stripe test API with test card 4242424242424242.
 */
test.describe('Stripe Sandbox - Successful Checkout', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let appName: string;
    const envName = 'Premium Test Environment';

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        appName = setup.appName;
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('can create a paid environment with PRO tier', async () => {
        // Navigate to the application
        await sharedPage.goto('/dashboard');
        await sharedPage.getByRole('button', {name: appName}).click();
        await sharedPage.waitForLoadState('networkidle');

        // Create a PRO tier environment (paid)
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        await sharedPage.locator('#envName').fill(envName);
        await sharedPage.locator('#envDesc').fill('Testing successful payment flow');

        // Select PRO tier (paid)
        const proTier = sharedPage.locator('.tier-option').filter({hasText: 'Pro'});
        await proTier.click();

        // Create the environment
        await sharedPage.locator('.environment-creation-overlay')
            .getByRole('button', {name: /Create Environment/}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();
    });

    test('cart shows pending PRO environment', async () => {
        // Navigate to checkout
        await sharedPage.goto('/dashboard/checkout');

        // Verify cart shows the pending environment
        const cartItem = sharedPage.locator('.cart-item').filter({hasText: envName});
        await expect(cartItem).toBeVisible();
        await expect(cartItem).toContainText('PRO');
        await expect(cartItem).toContainText('$50');
    });

    test('clicking proceed redirects to Stripe checkout', async () => {
        await sharedPage.getByRole('button', {name: 'Proceed to Payment'}).click();

        // Should redirect to real Stripe checkout
        await expect(sharedPage).toHaveURL(/checkout\.stripe\.com/, {timeout: 30000});
    });

    test('can complete payment with test card', async () => {
        // Wait for Stripe checkout to be ready by waiting for submit button
        const submitButton = sharedPage.locator('[data-testid="hosted-payment-submit-button"]')
            .or(sharedPage.getByRole('button', {name: /pay|subscribe/i}));
        await submitButton.waitFor({state: 'visible', timeout: 15000});

        // Fill email if required
        const emailField = sharedPage.locator('input[name="email"]');
        if (await emailField.isVisible({timeout: 2000}).catch(() => false)) {
            await emailField.fill('test@example.com');
        }

        // Fill card details using reusable helper
        await fillStripeCardDetails(sharedPage, TEST_CARDS.SUCCESS);

        // Submit payment
        await submitButton.click();
    });

    test('redirects to success page after payment', async () => {
        // Wait for redirect to success page (can take a while for Stripe to process)
        await expect(sharedPage).toHaveURL(/checkout\/success/, {timeout: 30000});

        // Verify success message - the page shows "Payment Successful!"
        const successMessage = sharedPage.getByText(/payment successful|thank you.*payment|success/i).first();
        await expect(successMessage).toBeVisible({timeout: 10000});
    });

    test('webhook processes and environment becomes paid', async () => {
        // Wait for webhook to process and cart to be empty
        await waitForCartEmpty(sharedPage, 30000);
    });

    test('billing shows active subscription', async () => {
        // Navigate to billing and verify subscription is active
        await waitForWebhookProcessing(sharedPage, 30000);
    });
});
