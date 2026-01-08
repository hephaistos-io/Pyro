import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';
import {TEST_CARDS, fillStripeCardDetails} from '../../utils/stripe-checkout.util';

/**
 * Sandbox tests for insufficient funds error handling.
 * Uses Stripe test card 4000000000009995 (insufficient funds).
 */
test.describe('Stripe Sandbox - Insufficient Funds', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let appName: string;
    const envName = 'Insufficient Funds Test';

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

        // Create a BUSINESS tier environment (paid - highest tier)
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        await sharedPage.locator('#envName').fill(envName);
        await sharedPage.locator('#envDesc').fill('Testing insufficient funds error');

        // Select BUSINESS tier
        const businessTier = sharedPage.locator('.tier-option').filter({hasText: 'Business'});
        await businessTier.click();

        await sharedPage.locator('.environment-creation-overlay')
            .getByRole('button', {name: /Create Environment/}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();
    });

    test('can navigate to checkout', async () => {
        await sharedPage.goto('/dashboard/checkout');

        const cartItem = sharedPage.locator('.cart-item').filter({hasText: envName});
        await expect(cartItem).toBeVisible();
        await expect(cartItem).toContainText('BUSINESS');
    });

    test('can proceed to Stripe checkout', async () => {
        await sharedPage.getByRole('button', {name: 'Proceed to Payment'}).click();
        await expect(sharedPage).toHaveURL(/checkout\.stripe\.com/, {timeout: 30000});
    });

    test('insufficient funds card shows specific error', async () => {
        // Wait for Stripe checkout to be ready by waiting for submit button
        const submitButton = sharedPage.locator('[data-testid="hosted-payment-submit-button"]')
            .or(sharedPage.getByRole('button', {name: /pay|subscribe/i}));
        await submitButton.waitFor({state: 'visible', timeout: 15000});

        // Fill email if required
        const emailField = sharedPage.locator('input[name="email"]');
        if (await emailField.isVisible({timeout: 2000}).catch(() => false)) {
            await emailField.fill('test@example.com');
        }

        // Fill card details with INSUFFICIENT_FUNDS test card
        await fillStripeCardDetails(sharedPage, TEST_CARDS.INSUFFICIENT_FUNDS);

        // Submit payment
        await submitButton.click();

        // Should show insufficient funds error
        await expect(sharedPage.getByText(/insufficient funds|not enough|declined|failed/i)).toBeVisible({timeout: 30000});
    });

    test('remains on Stripe checkout page', async () => {
        // Should still be on Stripe (not redirected)
        await expect(sharedPage).toHaveURL(/checkout\.stripe\.com/);
    });
});
