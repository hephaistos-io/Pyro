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
 * Fills Stripe card input fields in the hosted checkout page.
 * This is a public helper that can be used by individual test files.
 *
 * Stripe's hosted checkout (checkout.stripe.com) card fields are in iframes.
 * The selectors vary depending on Stripe's checkout version.
 *
 * @param page - Playwright page object (must be on checkout.stripe.com)
 * @param cardNumber - Test card number (e.g., TEST_CARDS.SUCCESS)
 * @param expiry - Expiry in MM/YY format (default: '12/34')
 * @param cvc - CVC code (default: '123')
 * @param cardholderName - Name on card (default: 'Test User')
 */
export async function fillStripeCardDetails(
    page: Page,
    cardNumber: string,
    expiry = '12/34',
    cvc = '123',
    cardholderName = 'Test User'
): Promise<void> {
    // Strategy: Try multiple selector patterns as Stripe's UI varies
    // Wait a moment for the page to fully load
    await page.waitForTimeout(2000);

    let cardFilled = false;

    // Approach 1: Try Stripe's Field-*Input IDs (newer checkout versions)
    const fieldNumberInput = page.locator('#Field-numberInput');
    if (await fieldNumberInput.isVisible({timeout: 2000}).catch(() => false)) {
        await fieldNumberInput.fill(cardNumber);
        await page.locator('#Field-expiryInput').fill(expiry);
        await page.locator('#Field-cvcInput').fill(cvc);
        cardFilled = true;
    }

    // Approach 2: Try ID-based selectors in iframes
    if (!cardFilled) {
        const iframes = page.locator('iframe');
        const iframeCount = await iframes.count();

        for (let i = 0; i < iframeCount; i++) {
            const frame = page.frameLocator(`iframe >> nth=${i}`);
            const fieldNumber = frame.locator('#Field-numberInput');
            if (await fieldNumber.isVisible({timeout: 500}).catch(() => false)) {
                await fieldNumber.fill(cardNumber);
                await frame.locator('#Field-expiryInput').fill(expiry);
                await frame.locator('#Field-cvcInput').fill(cvc);
                cardFilled = true;
                break;
            }
        }
    }

    // Approach 3: Try data-elements-stable-field-name selectors in iframes
    if (!cardFilled) {
        const iframes = page.locator('iframe');
        const iframeCount = await iframes.count();

        for (let i = 0; i < iframeCount; i++) {
            const frame = page.frameLocator(`iframe >> nth=${i}`);
            const cardInput = frame.locator('[data-elements-stable-field-name="cardNumber"]');
            if (await cardInput.isVisible({timeout: 500}).catch(() => false)) {
                await cardInput.fill(cardNumber);
                await frame.locator('[data-elements-stable-field-name="cardExpiry"]').fill(expiry);
                await frame.locator('[data-elements-stable-field-name="cardCvc"]').fill(cvc);
                cardFilled = true;
                break;
            }
        }
    }

    // Approach 4: Try name-based selectors in iframes
    if (!cardFilled) {
        const iframes = page.locator('iframe');
        const iframeCount = await iframes.count();

        for (let i = 0; i < iframeCount; i++) {
            const frame = page.frameLocator(`iframe >> nth=${i}`);
            const cardInput = frame.locator('input[name="cardnumber"]');
            if (await cardInput.isVisible({timeout: 500}).catch(() => false)) {
                await cardInput.fill(cardNumber);
                // Find expiry and CVC in any iframe
                for (let j = 0; j < iframeCount; j++) {
                    const otherFrame = page.frameLocator(`iframe >> nth=${j}`);
                    const expInput = otherFrame.locator('input[name="exp-date"]');
                    if (await expInput.isVisible({timeout: 200}).catch(() => false)) {
                        await expInput.fill(expiry);
                    }
                    const cvcInput = otherFrame.locator('input[name="cvc"]');
                    if (await cvcInput.isVisible({timeout: 200}).catch(() => false)) {
                        await cvcInput.fill(cvc);
                    }
                }
                cardFilled = true;
                break;
            }
        }
    }

    // Approach 5: Click on the card input area and use keyboard input
    if (!cardFilled) {
        // Find the card input area by looking for the placeholder text
        const cardNumberArea = page.getByPlaceholder(/1234.*1234.*1234.*1234/i).first()
            .or(page.locator('input[placeholder*="1234"]').first())
            .or(page.locator('[aria-label*="Card number"]').first());

        if (await cardNumberArea.isVisible({timeout: 2000}).catch(() => false)) {
            await cardNumberArea.click();
            await cardNumberArea.fill(cardNumber);
            // Tab to expiry
            await page.keyboard.press('Tab');
            await page.keyboard.type(expiry);
            // Tab to CVC
            await page.keyboard.press('Tab');
            await page.keyboard.type(cvc);
            cardFilled = true;
        }
    }

    if (!cardFilled) {
        throw new Error('Could not find Stripe card input fields. Check if the Stripe checkout page loaded correctly.');
    }

    // Fill cardholder name if required (shown on some checkout configurations)
    const cardholderInput = page.locator('#billingName, input[name="billingName"], input[placeholder*="name on card" i]').first();
    if (await cardholderInput.isVisible({timeout: 1000}).catch(() => false)) {
        await cardholderInput.fill(cardholderName);
    }
}

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
    await expect(page).toHaveURL(/checkout\/success/, {timeout: 30000});
}

/**
 * Fills in Stripe's hosted checkout form with test card details.
 *
 * Stripe's hosted checkout (checkout.stripe.com) uses different layouts depending on
 * the checkout mode and configuration. This function handles multiple selector patterns.
 *
 * @see https://docs.stripe.com/testing - Stripe test mode documentation
 * @see https://github.com/microsoft/playwright/issues/19709 - Known Stripe selectors
 */
async function fillStripeCheckoutForm(
    page: Page,
    card: { number: string; exp: string; cvc: string; zip: string; email: string }
): Promise<void> {
    // Wait for Stripe checkout page to be ready by waiting for the submit button
    const submitButton = page.locator('[data-testid="hosted-payment-submit-button"]')
        .or(page.getByRole('button', {name: /pay|subscribe/i}));
    await submitButton.waitFor({state: 'visible', timeout: 15000});

    // Email field (if present - depends on checkout session configuration)
    const emailField = page.locator('input[name="email"]');
    if (await emailField.isVisible({timeout: 2000}).catch(() => false)) {
        await emailField.fill(card.email);
    }

    // Fill card details using the reusable helper
    await fillStripeCardDetails(page, card.number, card.exp, card.cvc);

    // Click Pay/Subscribe button
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

/**
 * Waits for Stripe 3D Secure authentication popup and completes it.
 * In Stripe test mode, there's a "Complete" or "Authorize" button to simulate success.
 *
 * @param page - Playwright page object
 * @param timeout - Maximum time to wait for 3DS completion
 */
export async function complete3DSecure(page: Page, timeout = 30000): Promise<void> {
    // Stripe's 3DS test page shows a modal with "COMPLETE" and "FAIL" buttons
    // The modal can be either directly on the page or inside an iframe

    // First try direct page button (Stripe's test 3DS popup)
    const directCompleteButton = page.getByRole('button', {name: /^COMPLETE$/i})
        .or(page.getByRole('button', {name: /Complete|Authorize|Confirm/i}));

    if (await directCompleteButton.isVisible({timeout: 5000}).catch(() => false)) {
        await directCompleteButton.click();
    } else {
        // Try iframe-based 3DS
        const frame = page.frameLocator('iframe[name*="stripe-challenge"]')
            .or(page.frameLocator('iframe[src*="stripe.com/3d-secure"]'))
            .or(page.frameLocator('iframe[name*="__privateStripeFrame"]'));

        const completeButton = frame.getByRole('button', {name: /Complete|Authorize|Confirm/i})
            .or(frame.locator('[data-testid="complete-button"]'))
            .or(frame.locator('button[type="submit"]'));

        await completeButton.click({timeout});
    }

    // Wait for redirect back to success or checkout page
    await page.waitForURL(/checkout\/success|checkout$/, {timeout});
}

/**
 * Waits for webhook to be processed by checking environment status change.
 * Polls the billing page or dashboard until subscription shows as active.
 *
 * @param page - Playwright page object
 * @param timeout - Maximum time to wait for webhook processing (default 30s)
 * @param pollInterval - Time between polls (default 2s)
 */
export async function waitForWebhookProcessing(
    page: Page,
    timeout = 30000,
    pollInterval = 2000
): Promise<void> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
        // Navigate to billing page to check subscription status
        await page.goto('/dashboard/billing');
        await page.waitForLoadState('networkidle');

        // Check if subscription is active (no "no subscription" message)
        const noSubscription = page.locator('.no-subscription');
        const isNoSubscriptionVisible = await noSubscription.isVisible({timeout: 1000}).catch(() => false);

        if (!isNoSubscriptionVisible) {
            // Subscription exists - check if it's active
            const activeIndicator = page.locator('[data-status="active"], .subscription-active, .status-badge')
                .or(page.getByText(/active|subscribed/i));

            if (await activeIndicator.isVisible({timeout: 1000}).catch(() => false)) {
                return; // Webhook processed successfully
            }
        }

        // Wait before next poll
        await page.waitForTimeout(pollInterval);
    }

    throw new Error(`Webhook processing timeout after ${timeout}ms - subscription not active`);
}

/**
 * Waits for the cart badge to show zero or disappear (indicating environments are paid).
 *
 * @param page - Playwright page object
 * @param timeout - Maximum time to wait
 */
export async function waitForCartEmpty(
    page: Page,
    timeout = 30000
): Promise<void> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
        await page.reload();
        await page.waitForLoadState('networkidle');

        const cartBadge = page.locator('.cart-badge');
        const isHidden = !(await cartBadge.isVisible({timeout: 500}).catch(() => false));
        const badgeText = await cartBadge.textContent().catch(() => '0');

        if (isHidden || badgeText === '0' || badgeText === '') {
            return; // Cart is empty - payments processed
        }

        await page.waitForTimeout(2000);
    }

    throw new Error(`Cart still has items after ${timeout}ms - webhook may not have processed`);
}
