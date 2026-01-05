import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Cart Functionality', () => {
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

    test('cart link is disabled when cart is empty', async () => {
        // Cart link should be visible but disabled when there are no pending items
        const cartLink = sharedPage.locator('.cart-link');
        await expect(cartLink).toBeVisible();
        await expect(cartLink).toHaveClass(/cart-link--disabled/);

        // Cart badge should not be visible when empty
        const cartBadge = sharedPage.locator('.cart-badge');
        await expect(cartBadge).not.toBeVisible();
    });

    test('can create environment with FREE tier without adding to cart', async () => {
        // Click Create Environment button
        await sharedPage.locator('.add-env-btn').click();

        // Wait for overlay to appear
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        // Verify tier selector is visible
        await expect(sharedPage.locator('.tier-selector')).toBeVisible();

        // Verify FREE tier is selected by default
        const freeTierButton = sharedPage.locator('.tier-option').filter({hasText: 'Free'});
        await expect(freeTierButton).toHaveClass(/tier-option--selected/);

        // Fill in environment details
        await sharedPage.locator('#envName').fill('Test Free Environment');
        await sharedPage.locator('#envDesc').fill('Testing free tier');

        // Click Create Environment button (text should not include price for free)
        const createButton = sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: 'Create Environment'});
        await expect(createButton).toBeVisible();
        await createButton.click();

        // Wait for overlay to close
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();

        // Cart link should still be disabled (no paid items added)
        await expect(sharedPage.locator('.cart-link')).toHaveClass(/cart-link--disabled/);
    });

    test('can select a paid tier and see price update', async () => {
        // Click Create Environment button
        await sharedPage.locator('.add-env-btn').click();

        // Wait for overlay to appear
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        // Select BASIC tier
        const basicTierButton = sharedPage.locator('.tier-option').filter({hasText: 'Basic'});
        await basicTierButton.click();

        // Verify it's now selected
        await expect(basicTierButton).toHaveClass(/tier-option--selected/);

        // Verify submit button text includes price (use more specific selector to avoid matching tier button)
        const createButton = sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: /Create Environment.*\$10\/mo/});
        await expect(createButton).toBeVisible();

        // Cancel
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();
    });

    test('creating environment with paid tier adds it to cart', async () => {
        // Click Create Environment button
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        // Fill in environment details
        await sharedPage.locator('#envName').fill('Test Paid Environment');
        await sharedPage.locator('#envDesc').fill('Testing paid tier');

        // Select STANDARD tier ($25/mo)
        const standardTierButton = sharedPage.locator('.tier-option').filter({hasText: 'Standard'});
        await standardTierButton.click();

        // Click Create button
        await sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: /Create Environment/}).click();

        // Wait for overlay to close
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();

        // Wait for UI to update
        await sharedPage.waitForTimeout(1000);

        // Cart link should now be enabled (not disabled) with badge showing 1
        const cartLink = sharedPage.locator('.cart-link');
        await expect(cartLink).toBeVisible();
        await expect(cartLink).not.toHaveClass(/cart-link--disabled/);

        const cartBadge = sharedPage.locator('.cart-badge');
        await expect(cartBadge).toHaveText('1');
    });

    test('creating another paid environment increments cart count', async () => {
        // Click Create Environment button
        await sharedPage.locator('.add-env-btn').click();
        await expect(sharedPage.locator('.environment-creation-overlay')).toBeVisible();

        // Fill in environment details
        await sharedPage.locator('#envName').fill('Another Paid Environment');
        await sharedPage.locator('#envDesc').fill('Another paid tier test');

        // Select PRO tier ($50/mo)
        const proTierButton = sharedPage.locator('.tier-option').filter({hasText: 'Pro'});
        await proTierButton.click();

        // Click Create button
        await sharedPage.locator('.environment-creation-overlay').getByRole('button', {name: /Create Environment/}).click();

        // Wait for overlay to close
        await expect(sharedPage.locator('.environment-creation-overlay')).not.toBeVisible();

        // Wait for UI to update
        await sharedPage.waitForTimeout(1000);

        // Cart badge should now show 2
        const cartBadge = sharedPage.locator('.cart-badge');
        await expect(cartBadge).toHaveText('2');
    });

    test('clicking cart link navigates to checkout page', async () => {
        const cartLink = sharedPage.locator('.cart-link');
        await cartLink.click();

        // Should navigate to checkout page
        await expect(sharedPage).toHaveURL(/\/dashboard\/checkout/);

        // Should see the checkout page content
        await expect(sharedPage.getByRole('heading', {name: 'Checkout'})).toBeVisible();
    });

    test('checkout page shows pending environments', async () => {
        // Should show the cart items
        await expect(sharedPage.locator('.cart-item')).toHaveCount(2);

        // Should show the correct environment names
        await expect(sharedPage.locator('.cart-item').filter({hasText: 'Test Paid Environment'})).toBeVisible();
        await expect(sharedPage.locator('.cart-item').filter({hasText: 'Another Paid Environment'})).toBeVisible();

        // Should show total
        await expect(sharedPage.locator('.cart-total')).toBeVisible();
    });

    test('can remove item from cart', async () => {
        // Find the first remove button and click it
        const removeButton = sharedPage.locator('.remove-item-btn').first();
        await removeButton.click();

        // Should now have 1 item
        await expect(sharedPage.locator('.cart-item')).toHaveCount(1);

        // Cart badge should show 1
        const cartBadge = sharedPage.locator('.cart-badge');
        await expect(cartBadge).toHaveText('1');
    });

    test('clearing all items shows empty cart message', async () => {
        // Remove the remaining item
        const removeButton = sharedPage.locator('.remove-item-btn').first();
        await removeButton.click();

        // Should show empty cart message
        await expect(sharedPage.locator('.empty-cart')).toBeVisible();

        // Checkout button should be disabled or not visible
        const checkoutButton = sharedPage.getByRole('button', {name: 'Proceed to Payment'});
        await expect(checkoutButton).toBeDisabled();
    });
});
