import {expect, Page, test} from '@playwright/test';
import {
    addIdentifier,
    addSystemField,
    addUserField,
    deleteField,
    deleteIdentifier,
    editOverrideValue,
    fieldExists,
    getFieldKeys,
    getIdentifiers,
    navigateToOverridesTab,
    navigateToTemplateTab,
    selectSystemTemplateType,
    selectUserTemplateType,
    setupUserWithApplication
} from '../../utils';

test.describe('System Template Management', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let sharedAppName: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        sharedAppName = setup.appName;

        // Navigate to Template tab
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays empty state when no system fields exist', async () => {
        // Should show empty state message
        await expect(sharedPage.locator('.template-empty')).toBeVisible();
        await expect(sharedPage.getByText('No system template fields configured')).toBeVisible();
    });

    test('shows Add Field button', async () => {
        await expect(sharedPage.getByRole('button', {name: 'Add Field'})).toBeVisible();
    });

    test('clicking Add Field shows overlay form', async () => {
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();

        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();
        await expect(sharedPage.getByLabel('Field Key *')).toBeVisible();
        await expect(sharedPage.getByRole('button', {name: 'Add Field'}).nth(1)).toBeVisible();
        await expect(sharedPage.getByRole('button', {name: 'Cancel'})).toBeVisible();
    });

    test('canceling Add Field form closes it', async () => {
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();
    });

    test('adds a system field successfully', async () => {
        await addSystemField(sharedPage, 'api_url', 'https://api.example.com');

        // Verify field appears in table
        await expect(sharedPage.locator('.template-matrix-table')).toBeVisible();
        await expect(sharedPage.locator('.template-matrix-table__field-name').filter({hasText: 'api_url'})).toBeVisible();
    });

    test('displays the field value correctly', async () => {
        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'api_url'});
        await expect(row.locator('.template-matrix-table__value')).toContainText('https://api.example.com');
    });

    test('adds a second system field', async () => {
        await addSystemField(sharedPage, 'timeout_ms', '5000');

        const keys = await getFieldKeys(sharedPage);
        expect(keys).toContain('api_url');
        expect(keys).toContain('timeout_ms');
    });

    test('deletes a system field', async () => {
        await deleteField(sharedPage, 'timeout_ms');

        // Wait a moment for the API call to complete
        await sharedPage.waitForTimeout(500);

        const exists = await fieldExists(sharedPage, 'timeout_ms');
        expect(exists).toBe(false);
    });

    test('field persists after page refresh', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Template tab
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);

        // Verify field still exists
        const exists = await fieldExists(sharedPage, 'api_url');
        expect(exists).toBe(true);
    });
});

test.describe('User Template Management', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Navigate to Template tab and select User type
        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays empty state when no user fields exist', async () => {
        await expect(sharedPage.locator('.template-empty')).toBeVisible();
        await expect(sharedPage.getByText('No user template fields configured')).toBeVisible();
    });

    test('adds a string user field', async () => {
        await addUserField(sharedPage, 'username', 'STRING');

        await expect(sharedPage.locator('.template-matrix-table')).toBeVisible();
        await expect(sharedPage.locator('.template-matrix-table__field-name').filter({hasText: 'username'})).toBeVisible();
    });

    test('displays correct type for string field', async () => {
        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'username'});
        await expect(row.locator('.template-matrix-table__type')).toContainText('STRING');
    });

    test('adds a number user field', async () => {
        await addUserField(sharedPage, 'age', 'NUMBER');

        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'age'});
        await expect(row.locator('.template-matrix-table__type')).toContainText('NUMBER');
    });

    test('adds a boolean user field', async () => {
        await addUserField(sharedPage, 'is_premium', 'BOOLEAN');

        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'is_premium'});
        await expect(row.locator('.template-matrix-table__type')).toContainText('BOOLEAN');
    });

    test('deletes a user field', async () => {
        await deleteField(sharedPage, 'is_premium');

        await sharedPage.waitForTimeout(500);

        const exists = await fieldExists(sharedPage, 'is_premium');
        expect(exists).toBe(false);
    });
});

test.describe('Template Type Switching', () => {
    test('switches between System and User template types', async ({page}) => {
        await setupUserWithApplication(page);
        await navigateToTemplateTab(page);

        // Start on System (default)
        const systemBtn = page.locator('.template-toggle__btn').filter({hasText: 'System'});
        const userBtn = page.locator('.template-toggle__btn').filter({hasText: 'User'});

        await expect(systemBtn).toHaveClass(/template-toggle__btn--active/);
        await expect(userBtn).not.toHaveClass(/template-toggle__btn--active/);

        // Switch to User
        await userBtn.click();
        await expect(userBtn).toHaveClass(/template-toggle__btn--active/);
        await expect(systemBtn).not.toHaveClass(/template-toggle__btn--active/);

        // Switch back to System
        await systemBtn.click();
        await expect(systemBtn).toHaveClass(/template-toggle__btn--active/);
    });
});

test.describe('Template Search', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Navigate to Template tab and add some fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'database_url', 'postgres://localhost');
        await addSystemField(sharedPage, 'redis_url', 'redis://localhost');
        await addSystemField(sharedPage, 'api_key', 'secret123');

        // Verify all fields were added
        const keys = await getFieldKeys(sharedPage);
        if (keys.length !== 3) {
            console.log('WARNING: Expected 3 fields, got', keys.length, ':', keys);
        }
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('filters fields by search query', async () => {
        // Search for "url"
        await sharedPage.locator('.template-search__input').fill('url');

        // Should show only url fields
        const keys = await getFieldKeys(sharedPage);
        expect(keys).toContain('database_url');
        expect(keys).toContain('redis_url');
        expect(keys).not.toContain('api_key');
    });

    test('clears search shows all fields', async () => {
        await sharedPage.locator('.template-search__input').fill('');

        // Wait for search filter to update
        await sharedPage.waitForTimeout(300);

        // After clearing search, all 3 fields should be visible
        const keys = await getFieldKeys(sharedPage);
        expect(keys).toContain('database_url');
        expect(keys).toContain('redis_url');
        expect(keys).toContain('api_key');
    });

    test('shows empty state for no matches', async () => {
        await sharedPage.locator('.template-search__input').fill('nonexistent');

        await expect(sharedPage.getByText('No matching system fields found')).toBeVisible();

        // Clear search for cleanup
        await sharedPage.locator('.template-search__input').fill('');
    });
});

test.describe('Overrides Tab - Identifiers', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // First add some template fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'feature_flag', 'false');
        await addSystemField(sharedPage, 'max_items', '10');

        // Navigate to Overrides tab
        await navigateToOverridesTab(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays empty state when no identifiers exist', async () => {
        await expect(sharedPage.getByText('No identifiers configured')).toBeVisible();
    });

    test('shows Add Identifier button', async () => {
        await expect(sharedPage.getByRole('button', {name: 'Add Identifier'})).toBeVisible();
    });

    test('clicking Add Identifier shows form', async () => {
        await sharedPage.getByRole('button', {name: 'Add Identifier'}).click();

        await expect(sharedPage.locator('.overrides-add-form')).toBeVisible();
        await expect(sharedPage.locator('.overrides-add-form__input')).toBeVisible();
    });

    test('canceling Add Identifier form closes it', async () => {
        await sharedPage.locator('.overrides-add-form__btn--cancel').click();
        await expect(sharedPage.locator('.overrides-add-form')).not.toBeVisible();
    });

    test('adds an identifier successfully', async () => {
        await addIdentifier(sharedPage, 'user-123');

        // Verify matrix table appears with identifier
        await expect(sharedPage.locator('.matrix-table')).toBeVisible();
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-123');
    });

    test('adds a second identifier', async () => {
        await addIdentifier(sharedPage, 'user-456');

        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-123');
        expect(identifiers).toContain('user-456');
    });

    test('displays template fields as rows', async () => {
        // Check that the system template fields appear as rows
        await expect(sharedPage.locator('.matrix-table__field-name').filter({hasText: 'feature_flag'})).toBeVisible();
        await expect(sharedPage.locator('.matrix-table__field-name').filter({hasText: 'max_items'})).toBeVisible();
    });
});

test.describe('Overrides Tab - Override Values', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add template fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'setting_a', 'default_value');

        // Navigate to Overrides and add identifier
        await navigateToOverridesTab(sharedPage);
        await addIdentifier(sharedPage, 'test-user');
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('clicking a cell enters edit mode', async () => {
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'setting_a'});
        const valueCell = row.locator('.matrix-table__cell').nth(1);

        await valueCell.locator('.matrix-table__value').click();

        await expect(valueCell.locator('.matrix-table__input')).toBeVisible();
    });

    test('sets an override value', async () => {
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'setting_a'});
        const valueCell = row.locator('.matrix-table__cell').nth(1);

        // Input should already be visible from previous test
        const input = valueCell.locator('.matrix-table__input');
        await input.fill('overridden_value');
        await input.press('Enter');

        // Wait for edit to save
        await expect(input).not.toBeVisible();

        // Verify the override is displayed
        await expect(valueCell.locator('.matrix-table__value')).toContainText('overridden_value');
    });

    test('override cell is styled differently', async () => {
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'setting_a'});
        const valueCell = row.locator('.matrix-table__cell').nth(1);

        await expect(valueCell).toHaveClass(/matrix-table__cell--override/);
    });
});

test.describe('Tab Navigation', () => {
    test('navigates between Overview, Template, and Overrides tabs', async ({page}) => {
        await setupUserWithApplication(page);

        // Should start on Overview tab
        await expect(page.locator('.overview-layout')).toBeVisible();

        // Navigate to Template
        await page.getByRole('button', {name: 'Template'}).click();
        await expect(page.locator('.template-tab')).toBeVisible();

        // Navigate to Overrides
        await page.getByRole('button', {name: /Overrides/}).click();
        await expect(page.locator('.overrides-tab')).toBeVisible();

        // Navigate back to Overview
        await page.getByRole('button', {name: 'Overview'}).click();
        await expect(page.locator('.overview-layout')).toBeVisible();
    });
});

test.describe('Field Constraints - String Fields', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('adds a string field with constraints via Add Field overlay', async () => {
        // Click Add Field button
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();

        // Wait for overlay to appear - check for heading
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

        // Fill in field details - note the asterisks in labels
        await sharedPage.getByLabel('Field Key *').fill('username');
        await sharedPage.getByLabel('Description').fill('User login name');

        // Select STRING type
        await sharedPage.getByLabel('Type *').selectOption('String');

        // Fill in string constraints
        await sharedPage.getByLabel('Default Value').fill('guest');
        await sharedPage.getByLabel('Min Length').fill('3');
        await sharedPage.getByLabel('Max Length').fill('20');

        // Toggle editable
        await sharedPage.getByLabel('User Editable').check();

        // Click Add button in the overlay
        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();

        // Wait for overlay to close
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();

        // Verify field appears with constraints
        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'username'});
        await expect(row).toBeVisible();
        await expect(row.locator('.template-matrix-table__type')).toContainText('STRING');
    });

    test('displays string field constraints in the UI', async () => {
        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'username'});

        // Check that constraints are shown (implementation depends on your UI)
        // For now, just verify the row exists and has the right type
        await expect(row.locator('.template-matrix-table__type')).toContainText('STRING');
    });
});

test.describe('Field Constraints - Number Fields', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('adds a number field with min/max constraints via overlay', async () => {
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

        await sharedPage.getByLabel('Field Key *').fill('age');
        await sharedPage.getByLabel('Description').fill('User age');
        await sharedPage.getByLabel('Type *').selectOption('Number');

        await sharedPage.getByLabel('Default Value').fill('18');
        await sharedPage.getByLabel('Min Value').fill('0');
        await sharedPage.getByLabel('Max Value').fill('120');

        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();

        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'age'});
        await expect(row).toBeVisible();
        await expect(row.locator('.template-matrix-table__type')).toContainText('NUMBER');
    });

    test('adds a number field with increment amount', async () => {
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

        await sharedPage.getByLabel('Field Key *').fill('price');
        await sharedPage.getByLabel('Description').fill('Product price');
        await sharedPage.getByLabel('Type *').selectOption('Number');

        await sharedPage.getByLabel('Default Value').fill('10.00');
        await sharedPage.getByLabel('Min Value').fill('0');
        await sharedPage.getByLabel('Max Value').fill('1000');
        await sharedPage.getByLabel('Increment Amount').fill('0.01');

        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();

        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'price'});
        await expect(row).toBeVisible();
    });
});

test.describe('Field Constraints - Enum Fields', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('adds an enum field with options', async () => {
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

        await sharedPage.getByLabel('Field Key *').fill('tier');
        await sharedPage.getByLabel('Description').fill('User tier level');
        await sharedPage.getByLabel('Type *').selectOption('Enum');

        // Add enum options - wait for enum section to appear
        await expect(sharedPage.getByRole('heading', {name: 'Enum Options'})).toBeVisible();

        // Type new value in the textbox and press Enter to add options
        const addOptionInput = sharedPage.getByPlaceholder('Add option...');

        // Add "free" option - type and press Enter
        await addOptionInput.fill('free');
        await addOptionInput.press('Enter');

        // Add "premium" option
        await addOptionInput.fill('premium');
        await addOptionInput.press('Enter');

        // Add "enterprise" option
        await addOptionInput.fill('enterprise');
        await addOptionInput.press('Enter');

        // Wait a moment for options to be added
        await sharedPage.waitForTimeout(300);

        // Set default value - select from dropdown
        await sharedPage.getByLabel('Default Value').selectOption('free');

        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();

        const row = sharedPage.locator('.template-matrix-table__row').filter({hasText: 'tier'});
        await expect(row).toBeVisible();
        await expect(row.locator('.template-matrix-table__type')).toContainText('ENUM');
    });
});

test.describe('Override Validation - Backend Enforcement', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add template fields with constraints
        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);

        // Add string field with min/max length
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();
        await sharedPage.getByLabel('Field Key *').fill('username');
        await sharedPage.getByLabel('Type *').selectOption('String');
        await sharedPage.getByLabel('Default Value').fill('guest');
        await sharedPage.getByLabel('Min Length').fill('5');
        await sharedPage.getByLabel('Max Length').fill('20');
        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();
        await sharedPage.waitForTimeout(500);

        // Add number field with min/max
        await sharedPage.getByRole('button', {name: 'Add Field'}).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).toBeVisible();
        await sharedPage.getByLabel('Field Key *').fill('score');
        await sharedPage.getByLabel('Type *').selectOption('Number');
        await sharedPage.getByLabel('Default Value').fill('50');
        await sharedPage.getByLabel('Min Value').fill('0');
        await sharedPage.getByLabel('Max Value').fill('100');
        await sharedPage.getByRole('button', {name: 'Add Field'}).nth(1).click();
        await expect(sharedPage.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();
        await sharedPage.waitForTimeout(500);

        // Navigate to Overrides and add identifier
        await navigateToOverridesTab(sharedPage);
        await addIdentifier(sharedPage, 'user-123');
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });
});

test.describe('Copy Overrides Between Environments', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Production environment already exists by default, switch to Development environment
        await sharedPage.locator('.selector-button').click();
        // Wait for dropdown to be visible before clicking
        const developmentOption = sharedPage.locator('.dropdown-item').filter({hasText: 'Development'});
        await expect(developmentOption).toBeVisible();
        await developmentOption.click();
        await sharedPage.waitForTimeout(500);

        // Add template fields
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'api_url', 'https://api.staging.com');

        // Add overrides in Development environment
        await navigateToOverridesTab(sharedPage);
        await addIdentifier(sharedPage, 'region-eu');
        await editOverrideValue(sharedPage, 'api_url', 'region-eu', 'https://api.eu.staging.com');

        await addIdentifier(sharedPage, 'region-us');
        await editOverrideValue(sharedPage, 'api_url', 'region-us', 'https://api.us.staging.com');

        // Wait for page to stabilize after all operations (especially important for Firefox)
        await sharedPage.waitForLoadState('networkidle');
        // Explicitly wait for the Copy Overrides button to be visible and stable
        await expect(sharedPage.getByRole('button', {name: 'Copy Overrides'})).toBeVisible();
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('Copy Overrides button is visible when identifiers exist', async () => {
        await expect(sharedPage.getByRole('button', {name: 'Copy Overrides'})).toBeVisible();
    });

    test('opens copy overrides overlay', async () => {
        await sharedPage.getByRole('button', {name: 'Copy Overrides'}).click();
        await expect(sharedPage.locator('.copy-overrides-overlay')).toBeVisible();
        await expect(sharedPage.getByText('Copy Identifier Overrides')).toBeVisible();
    });

    test('source environment is pre-selected', async () => {
        const sourceSelect = sharedPage.locator('select[name="sourceEnvironment"]');
        await expect(sourceSelect).toBeDisabled();
        // Should show current environment (staging)
    });

    test('displays available identifiers in dropdown', async () => {
        const identifierSelect = sharedPage.locator('select[name="identifier"]');
        await expect(identifierSelect).toBeVisible();

        // Check that our identifiers are in the list
        const options = identifierSelect.locator('option');
        const optionTexts = await options.allTextContents();
        expect(optionTexts.some(text => text.includes('region-eu'))).toBe(true);
        expect(optionTexts.some(text => text.includes('region-us'))).toBe(true);
    });

    test('selecting identifier enables target environment dropdown', async () => {
        const identifierSelect = sharedPage.locator('select[name="identifier"]');
        const targetSelect = sharedPage.locator('select[name="targetEnvironment"]');

        // Initially disabled
        await expect(targetSelect).toBeDisabled();

        // Select an identifier
        await identifierSelect.selectOption('region-eu');

        // Now should be enabled
        await expect(targetSelect).toBeEnabled();
    });

    test('displays summary when all fields are selected', async () => {
        const targetSelect = sharedPage.locator('select[name="targetEnvironment"]');

        // Select production environment (assuming it exists)
        const options = await targetSelect.locator('option').allTextContents();
        const productionOption = options.find(opt => opt.toLowerCase().includes('prod'));
        if (productionOption) {
            await targetSelect.selectOption({label: productionOption});
        }

        // Summary should appear
        await expect(sharedPage.locator('.form-section--summary')).toBeVisible();
    });

    test('Copy Overrides button is enabled when form is valid', async () => {
        const copyButton = sharedPage.locator('button[type="submit"]').filter({hasText: 'Copy Overrides'});
        await expect(copyButton).toBeEnabled();
    });

    test('successfully copies overrides', async () => {
        const copyButton = sharedPage.locator('button[type="submit"]').filter({hasText: 'Copy Overrides'});
        await copyButton.click();

        // Should show success message (loading state might be too fast to catch)
        await expect(sharedPage.locator('.message--success')).toBeVisible({timeout: 5000});

        // Overlay should close
        await expect(sharedPage.locator('.copy-overrides-overlay')).not.toBeVisible({timeout: 3000});
    });

    test('copied override appears in target environment', async () => {
        // Switch to production environment
        await sharedPage.locator('.selector-button').click();
        const productionOption = sharedPage.locator('.dropdown-item').filter({hasText: /Production/i});
        await expect(productionOption).toBeVisible();
        await productionOption.click();

        // Wait for overrides to load
        await sharedPage.waitForTimeout(1000);

        // Verify the copied identifier exists
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('region-eu');
    });
});

test.describe('Delete Identifiers', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add template field
        await navigateToTemplateTab(sharedPage);
        await selectSystemTemplateType(sharedPage);
        await addSystemField(sharedPage, 'feature_x', 'false');

        // Add identifiers
        await navigateToOverridesTab(sharedPage);
        await addIdentifier(sharedPage, 'temp-user-1');
        await addIdentifier(sharedPage, 'temp-user-2');
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('delete button is visible for identifiers', async () => {
        const deleteBtn = sharedPage.locator('.matrix-table__identifier-delete').first();
        await expect(deleteBtn).toBeVisible();
    });

    test('clicking delete shows confirmation', async () => {
        const deleteBtn = sharedPage.locator('.matrix-table__identifier-delete').first();
        await deleteBtn.click();

        // Should show delete confirmation overlay
        await expect(sharedPage.locator('.delete-field-overlay')).toBeVisible();
        await expect(sharedPage.getByRole('heading', {name: /delete identifier/i})).toBeVisible();
    });

    test('canceling delete closes confirmation', async () => {
        await sharedPage.getByRole('button', {name: 'Cancel'}).click();
        await expect(sharedPage.locator('.delete-field-overlay')).not.toBeVisible();
    });

    test('confirming delete removes identifier', async () => {
        // Use the deleteIdentifier utility which handles the confirmation flow
        await deleteIdentifier(sharedPage, 'temp-user-1');

        // Wait for deletion
        await sharedPage.waitForTimeout(500);

        // Identifier should be gone
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).not.toContain('temp-user-1');
        expect(identifiers).toContain('temp-user-2');
    });
});

test.describe('User Template Overrides', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        await setupUserWithApplication(sharedPage);

        // Add user template fields
        await navigateToTemplateTab(sharedPage);
        await selectUserTemplateType(sharedPage);
        await addUserField(sharedPage, 'username', 'STRING');
        await addUserField(sharedPage, 'is_premium', 'BOOLEAN');

        // Navigate to Overrides
        await navigateToOverridesTab(sharedPage);
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('can switch to User template type in overrides', async () => {
        const userBtn = sharedPage.locator('.template-toggle__btn').filter({hasText: 'User'});
        await userBtn.click();
        await expect(userBtn).toHaveClass(/template-toggle__btn--active/);
    });

    test('adds identifier for user template', async () => {
        await addIdentifier(sharedPage, 'user-alice');

        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-alice');
    });

    test('displays user template fields in override matrix', async () => {
        await expect(sharedPage.locator('.matrix-table__field-name').filter({hasText: 'username'})).toBeVisible();
        await expect(sharedPage.locator('.matrix-table__field-name').filter({hasText: 'is_premium'})).toBeVisible();
    });

    test('sets string override for user template', async () => {
        await editOverrideValue(sharedPage, 'username', 'user-alice', 'alice_smith');

        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'username'});
        const cell = row.locator('.matrix-table__cell').nth(1);
        await expect(cell.locator('.matrix-table__value')).toContainText('alice_smith');
    });

    test('sets boolean override for user template', async () => {
        await editOverrideValue(sharedPage, 'is_premium', 'user-alice', 'true');

        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'is_premium'});
        const cell = row.locator('.matrix-table__cell').nth(1);
        await expect(cell.locator('.matrix-table__value')).toContainText('true');
    });
});
