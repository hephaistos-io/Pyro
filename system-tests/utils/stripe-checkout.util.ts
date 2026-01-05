import {expect, Page} from '@playwright/test';

/**
 * Stripe test card numbers for sandbox testing.
 * @see https://docs.stripe.com/testing
 */
export const TEST_CARDS = {
    /** Successful payment */
    SUCCESS: '4242424242424242',
    /** Payment requires authentication */
    REQUIRES_AUTH: '4000002500003155',
    /** Card declined */
    DECLINED: '4000000000000002',
    /** Insufficient funds */
    INSUFFICIENT_FUNDS: '4000000000009995',
    /** Attach succeeds, charge fails (good for testing failed subscriptions) */
    ATTACH_OK_CHARGE_FAIL: '4000000000000341',
};

const DEFAULT_TEST_CARD = {
    NUMBER: TEST_CARDS.SUCCESS,
    EXP: '12/34',
    CVC: '123',
    ZIP: '12345',
    EMAIL: 'test@example.com',
};

/**
 * Completes checkout flow - handles both mock and sandbox modes.
 *
 * In mock mode: Checkout redirects directly to success page (no Stripe UI).
 * In sandbox mode: Fills in Stripe's hosted checkout form with test card.
 *
 * @param page - Playwright page object
 * @param options - Optional card details for sandbox mode
 */
export async function completeCheckout(
    page: Page,
    options: {
        cardNumber?: string;
        expiry?: string;
        cvc?: string;
        zip?: string;
        email?: string;
    } = {}
): Promise<void> {
    const card = {
        number: options.cardNumber ?? DEFAULT_TEST_CARD.NUMBER,
        exp: options.expiry ?? DEFAULT_TEST_CARD.EXP,
        cvc: options.cvc ?? DEFAULT_TEST_CARD.CVC,
        zip: options.zip ?? DEFAULT_TEST_CARD.ZIP,
        email: options.email ?? DEFAULT_TEST_CARD.EMAIL,
    };

    // Click proceed to payment
    await page.getByRole('button', {name: 'Proceed to Payment'}).click();

    // Wait for navigation - could be to Stripe or to success page
    await page.waitForURL(/checkout\.stripe\.com|checkout\/success/, {timeout: 30000});

    // Check if we're on Stripe's checkout page (sandbox mode)
    if (page.url().includes('checkout.stripe.com')) {
        await fillStripeCheckoutForm(page, card);
    }

    // Wait for redirect back to success page
    await expect(page).toHaveURL(/checkout\/success/, {timeout: 60000});
}

/**
 * Fills in Stripe's hosted checkout form with test card details.
 */
async function fillStripeCheckoutForm(
    page: Page,
    card: { number: string; exp: string; cvc: string; zip: string; email: string }
): Promise<void> {
    // Wait for Stripe checkout to load
    await page.waitForLoadState('networkidle');

    // Email field (if present - depends on checkout session configuration)
    const emailField = page.locator('input[name="email"]');
    if (await emailField.isVisible({timeout: 2000}).catch(() => false)) {
        await emailField.fill(card.email);
    }

    // Card number - Stripe uses iframes for card fields
    // The iframe name pattern may vary, try common patterns
    const cardFrame = page.frameLocator('iframe[name*="privateStripeFrame"]').first()
        || page.frameLocator('iframe[title*="Secure card"]').first();

    // Fill card number
    const cardNumberInput = cardFrame.locator('[data-testid="card-number-input"]')
        .or(cardFrame.locator('input[name="cardnumber"]'))
        .or(cardFrame.locator('[placeholder*="Card number"]'));
    await cardNumberInput.fill(card.number);

    // Fill expiry (MM/YY format)
    const expiryInput = cardFrame.locator('[data-testid="card-expiry-input"]')
        .or(cardFrame.locator('input[name="exp-date"]'))
        .or(cardFrame.locator('[placeholder*="MM"]'));
    await expiryInput.fill(card.exp);

    // Fill CVC
    const cvcInput = cardFrame.locator('[data-testid="card-cvc-input"]')
        .or(cardFrame.locator('input[name="cvc"]'))
        .or(cardFrame.locator('[placeholder*="CVC"]'));
    await cvcInput.fill(card.cvc);

    // Fill billing ZIP if required
    const zipField = cardFrame.locator('[data-testid="postal-code-input"]')
        .or(cardFrame.locator('input[name="postal"]'))
        .or(cardFrame.locator('[placeholder*="ZIP"]'));
    if (await zipField.isVisible({timeout: 1000}).catch(() => false)) {
        await zipField.fill(card.zip);
    }

    // Click Pay/Subscribe button
    const submitButton = page.locator('[data-testid="hosted-payment-submit-button"]')
        .or(page.getByRole('button', {name: /pay|subscribe/i}));
    await submitButton.click();
}

/**
 * Cancels checkout and returns to the cancel URL.
 * Use this to test the cancel flow.
 */
export async function cancelCheckout(page: Page): Promise<void> {
    // Click proceed to payment first
    await page.getByRole('button', {name: 'Proceed to Payment'}).click();

    // Wait for Stripe checkout page
    await page.waitForURL(/checkout\.stripe\.com/, {timeout: 30000});

    // Click back/cancel button on Stripe's page
    const backButton = page.locator('[data-testid="back-button"]')
        .or(page.getByRole('link', {name: /back/i}));
    await backButton.click();

    // Wait for redirect back to our cancel URL (checkout page)
    await expect(page).toHaveURL(/checkout$/, {timeout: 30000});
}
