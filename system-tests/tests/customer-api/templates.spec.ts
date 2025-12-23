import {expect, Page, test} from '@playwright/test';
import {
    addIdentifier,
    addSystemField,
    editOverrideValue,
    navigateToOverridesTab,
    navigateToTemplateTab,
    selectSystemTemplateType,
    setupUserWithApplication
} from '../../utils';

const CUSTOMER_API_BASE = '';

/**
 * Helper to get the Read-Key from the webapp UI
 */
async function getReadApiKey(page: Page): Promise<string> {
    // Navigate to Overview tab (should already be there after setup)
    await page.getByRole('button', {name: 'Overview', exact: true}).click();
    await expect(page.locator('.overview-cards')).toBeVisible();

    const readKeySection = page.locator('.stat').filter({hasText: 'Read-Key'});

    // Click show button to reveal the key
    await readKeySection.getByRole('button', {name: 'Show key'}).click();

    // Wait for the actual key to appear (64 hex characters)
    const keyCode = readKeySection.locator('.stat__code--key');
    await expect(keyCode).toContainText(/[a-f0-9]{64}/i);

    const keyText = await keyCode.textContent();
    return keyText?.trim() || '';
}

/**
 * Helper to make authenticated requests to customer-api
 */
async function fetchWithApiKey(
    page: Page,
    path: string,
    apiKey: string
): Promise<{ status: number; body: unknown }> {
    const baseUrl = process.env.BASE_URL || 'http://localhost';
    const url = `${baseUrl}${CUSTOMER_API_BASE}${path}`;

    const response = await page.request.get(url, {
        headers: {
            'X-API-Key': apiKey
        }
    });

    let body: unknown;
    try {
        body = await response.json();
    } catch {
        body = await response.text();
    }

    return {status: response.status(), body};
}

test.describe('Customer API - Templates Endpoint', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let apiKey: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add system template fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'api_url', 'https://default.api.com');
        await addSystemField(sharedPage, 'timeout_ms', '5000');

        // Get the API key
        apiKey = await getReadApiKey(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('returns 401 without API key', async () => {
        const baseUrl = process.env.BASE_URL || 'http://localhost';
        const url = `${baseUrl}${CUSTOMER_API_BASE}/v1/api/templates/system`;

        const response = await sharedPage.request.get(url);
        expect(response.status()).toBe(401);
    });

    test('returns 401 with invalid API key', async () => {
        const {status} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            'invalid-key-that-does-not-exist'
        );
        expect(status).toBe(401);
    });

    test('returns 200 with valid API key', async () => {
        const {status} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            apiKey
        );
        expect(status).toBe(200);
    });

    test('returns SYSTEM template type', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            apiKey
        ) as { body: { type: string } };

        expect(body.type).toBe('SYSTEM');
    });

    test('returns default values from schema', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            apiKey
        ) as { body: { values: Record<string, unknown> } };

        expect(body.values).toHaveProperty('api_url', 'https://default.api.com');
        expect(body.values).toHaveProperty('timeout_ms', '5000');
    });

    test('returns schema with fields', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            apiKey
        ) as { body: { schema: { fields: unknown[] } } };

        expect(body.schema).toBeDefined();
        expect(body.schema.fields).toBeInstanceOf(Array);
        expect(body.schema.fields.length).toBe(2);
    });

    test('returns null appliedIdentifier when no identifier provided', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system',
            apiKey
        ) as { body: { appliedIdentifier: string | null } };

        expect(body.appliedIdentifier).toBeNull();
    });

    test('returns null appliedIdentifier for non-existent identifier', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system?identifier=non-existent',
            apiKey
        ) as { body: { appliedIdentifier: string | null; values: Record<string, unknown> } };

        expect(body.appliedIdentifier).toBeNull();
        // Should still return defaults
        expect(body.values).toHaveProperty('api_url', 'https://default.api.com');
    });
});

test.describe('Customer API - Templates with Overrides', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let apiKey: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add system template fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'api_url', 'https://default.api.com');
        await addSystemField(sharedPage, 'region', 'us-east');

        // Add overrides
        await navigateToOverridesTab(sharedPage);
        await addIdentifier(sharedPage, 'region-eu');
        await editOverrideValue(sharedPage, 'api_url', 'region-eu', 'https://eu.api.com');

        await addIdentifier(sharedPage, 'region-asia');
        await editOverrideValue(sharedPage, 'api_url', 'region-asia', 'https://asia.api.com');
        await editOverrideValue(sharedPage, 'region', 'region-asia', 'ap-southeast');

        // Get the API key
        apiKey = await getReadApiKey(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('applies override for existing identifier', async () => {
        const {status, body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system?identifier=region-eu',
            apiKey
        ) as { status: number; body: { values: Record<string, unknown>; appliedIdentifier: string } };

        expect(status).toBe(200);
        expect(body.values).toHaveProperty('api_url', 'https://eu.api.com');
        expect(body.values).toHaveProperty('region', 'us-east'); // Not overridden
        expect(body.appliedIdentifier).toBe('region-eu');
    });

    test('applies multiple field overrides for identifier', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system?identifier=region-asia',
            apiKey
        ) as { body: { values: Record<string, unknown>; appliedIdentifier: string } };

        expect(body.values).toHaveProperty('api_url', 'https://asia.api.com');
        expect(body.values).toHaveProperty('region', 'ap-southeast');
        expect(body.appliedIdentifier).toBe('region-asia');
    });

    test('returns defaults for non-existent identifier', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system?identifier=non-existent',
            apiKey
        ) as { body: { values: Record<string, unknown>; appliedIdentifier: string | null } };

        expect(body.values).toHaveProperty('api_url', 'https://default.api.com');
        expect(body.values).toHaveProperty('region', 'us-east');
        expect(body.appliedIdentifier).toBeNull();
    });

    test('empty identifier string returns defaults', async () => {
        const {body} = await fetchWithApiKey(
            sharedPage,
            '/v1/api/templates/system?identifier=',
            apiKey
        ) as { body: { values: Record<string, unknown>; appliedIdentifier: string | null } };

        expect(body.values).toHaveProperty('api_url', 'https://default.api.com');
        expect(body.appliedIdentifier).toBeNull();
    });
});

test.describe('Customer API - Templates Environment Isolation', () => {
    test('different environments have isolated overrides', async ({page}) => {
        await setupUserWithApplication(page);

        // Add template field
        await navigateToTemplateTab(page);
        await selectSystemTemplateType(page);
        await addSystemField(page, 'config_value', 'default');

        // Switch to Development environment and add override
        await page.locator('.selector-button').click();
        const developmentOption = page.locator('.dropdown-item').filter({hasText: 'Development'});
        await expect(developmentOption).toBeVisible();
        await developmentOption.click();
        await page.waitForTimeout(500);

        await navigateToOverridesTab(page);
        await addIdentifier(page, 'test-user');
        await editOverrideValue(page, 'config_value', 'test-user', 'dev-override');

        // Get Dev API key
        const devApiKey = await getReadApiKey(page);

        // Verify override applies in Dev
        const {body: devBody} = await fetchWithApiKey(
            page,
            '/v1/api/templates/system?identifier=test-user',
            devApiKey
        ) as { body: { values: Record<string, unknown>; appliedIdentifier: string } };

        expect(devBody.values).toHaveProperty('config_value', 'dev-override');
        expect(devBody.appliedIdentifier).toBe('test-user');

        // Switch to Production environment
        await page.locator('.selector-button').click();
        const productionOption = page.locator('.dropdown-item').filter({hasText: 'Production'});
        await expect(productionOption).toBeVisible();
        await productionOption.click();
        await page.waitForTimeout(500);

        // Get Production API key (different from Dev)
        const prodApiKey = await getReadApiKey(page);

        // Verify override does NOT exist in Production (identifier not found)
        const {body: prodBody} = await fetchWithApiKey(
            page,
            '/v1/api/templates/system?identifier=test-user',
            prodApiKey
        ) as { body: { values: Record<string, unknown>; appliedIdentifier: string | null } };

        expect(prodBody.values).toHaveProperty('config_value', 'default');
        expect(prodBody.appliedIdentifier).toBeNull();
    });
});
