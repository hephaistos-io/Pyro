import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';
import {TEST_CARDS, fillStripeCardDetails} from '../../utils/stripe-checkout.util';

/**
 * Sandbox tests for declined card handling.
 * Uses Stripe test card 4000000000000002 (always declined).
 */
test.describe('Stripe Sandbox - Declined Card', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let appName: string;
    const envName = 'Declined Test Environment';

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

        // Create a STANDARD tier environment (paid)
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        await sharedPage.locator('#envName').fill(envName);
        await sharedPage.locator('#envDesc').fill('Testing declined card flow');

        // Select STANDARD tier
        const standardTier = sharedPage.locator('.tier-option').filter({hasText: 'Standard'});
        await standardTier.click();

        await sharedPage.locator('.environment-creation-overlay')
            .getByRole('button', {name: /Create Environment/}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();
    });

    test('can navigate to checkout', async () => {
        await sharedPage.goto('/dashboard/checkout');

        // Verify cart shows the pending environment
        const cartItem = sharedPage.locator('.cart-item').filter({hasText: envName});
        await expect(cartItem).toBeVisible();
    });

    test('can proceed to Stripe checkout', async () => {
        await sharedPage.getByRole('button', {name: 'Proceed to Payment'}).click();
        await expect(sharedPage).toHaveURL(/checkout\.stripe\.com/, {timeout: 30000});
    });

    test('declined card shows error message', async () => {
        // Wait for Stripe checkout to be ready by waiting for submit button
        const submitButton = sharedPage.locator('[data-testid="hosted-payment-submit-button"]')
            .or(sharedPage.getByRole('button', {name: /pay|subscribe/i}));
        await submitButton.waitFor({state: 'visible', timeout: 15000});

        // Fill email if required
        const emailField = sharedPage.locator('input[name="email"]');
        if (await emailField.isVisible({timeout: 2000}).catch(() => false)) {
            await emailField.fill('test@example.com');
        }

        // Fill card details with DECLINED test card
        await fillStripeCardDetails(sharedPage, TEST_CARDS.DECLINED);

        // Submit payment
        await submitButton.click();

        // Should show declined error on Stripe's page
        await expect(sharedPage.getByText(/declined|unsuccessful|failed|error/i)).toBeVisible({timeout: 30000});
    });

    test('remains on Stripe checkout after decline', async () => {
        // Should still be on Stripe (not redirected to success)
        await expect(sharedPage).toHaveURL(/checkout\.stripe\.com/);
    });

    test('can retry with different card', async () => {
        // The form should still be available for retry
        const submitButton = sharedPage.locator('[data-testid="hosted-payment-submit-button"]')
            .or(sharedPage.getByRole('button', {name: /pay|subscribe|try again/i}));
        await expect(submitButton).toBeVisible();
    });
});
