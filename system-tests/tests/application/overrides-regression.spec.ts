import {expect, Page, test} from '@playwright/test';
import {
    addIdentifier,
    addSystemField,
    editOverrideValue,
    getIdentifiers,
    navigateToOverridesTab,
    navigateToTemplateTab,
    selectSystemTemplateType,
    setupUserWithApplication
} from '../../utils';

test.describe('Overrides - Identifier Persistence Regression Tests', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let sharedAppName: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        sharedAppName = setup.appName;

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

    test('adds first identifier successfully', async () => {
        await addIdentifier(sharedPage, 'user-001');

        // Verify matrix table appears with identifier
        await expect(sharedPage.locator('.matrix-table')).toBeVisible();
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
    });

    test('first identifier persists after page refresh', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Overrides tab
        await navigateToOverridesTab(sharedPage);

        // Verify identifier still exists
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
    });

    test('adds second identifier successfully', async () => {
        await addIdentifier(sharedPage, 'user-002');

        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');
    });

    test('second identifier persists after page refresh', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Overrides tab
        await navigateToOverridesTab(sharedPage);

        // Verify both identifiers still exist
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');
    });

    test('editing a field value does not delete the identifier', async () => {
        // Edit the feature_flag value for user-001
        await editOverrideValue(sharedPage, 'feature_flag', 'user-001', 'true');

        // Wait for edit to save
        await sharedPage.waitForTimeout(500);

        // Verify the identifier still exists
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');

        // Verify the value was updated
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'feature_flag'});
        const cell = row.locator('.matrix-table__cell').nth(1); // First identifier column
        await expect(cell.locator('.matrix-table__value')).toContainText('true');
    });

    test('identifier persists after editing value and refreshing', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Overrides tab
        await navigateToOverridesTab(sharedPage);

        // Verify both identifiers still exist
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');

        // Verify the edited value persisted
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'feature_flag'});
        const cell = row.locator('.matrix-table__cell').nth(1);
        await expect(cell.locator('.matrix-table__value')).toContainText('true');
    });

    test('setting value back to default removes override but keeps identifier', async () => {
        // Set the value back to the default (false)
        await editOverrideValue(sharedPage, 'feature_flag', 'user-001', 'false');

        // Wait for edit to save
        await sharedPage.waitForTimeout(500);

        // Verify the identifier still exists (this is the bug we're testing against)
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');

        // The cell should show the default dash indicator
        const row = sharedPage.locator('.matrix-table__row').filter({hasText: 'feature_flag'});
        const cell = row.locator('.matrix-table__cell').nth(1);
        await expect(cell.locator('.matrix-table__dash')).toBeVisible();
    });

    test('identifier persists after setting value to default and refreshing', async () => {
        // Navigate away and back
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Overrides tab
        await navigateToOverridesTab(sharedPage);

        // Verify both identifiers still exist (THIS IS THE KEY TEST)
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');
    });

    test('editing multiple fields for same identifier keeps identifier', async () => {
        // Edit max_items for user-002
        await editOverrideValue(sharedPage, 'max_items', 'user-002', '25');

        // Edit feature_flag for user-002
        await editOverrideValue(sharedPage, 'feature_flag', 'user-002', 'true');

        // Wait for edits to save
        await sharedPage.waitForTimeout(500);

        // Verify the identifier still exists
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-002');
    });

    test('adding third identifier after editing values works correctly', async () => {
        await addIdentifier(sharedPage, 'user-003');

        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');
        expect(identifiers).toContain('user-003');
    });

    test('all identifiers and their values persist after final refresh', async () => {
        // Navigate away and back one last time
        await sharedPage.goto('/dashboard');
        await expect(sharedPage.getByRole('button', {name: sharedAppName})).toBeVisible();
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Navigate back to Overrides tab
        await navigateToOverridesTab(sharedPage);

        // Verify all three identifiers exist
        const identifiers = await getIdentifiers(sharedPage);
        expect(identifiers).toContain('user-001');
        expect(identifiers).toContain('user-002');
        expect(identifiers).toContain('user-003');

        // Verify user-002's values persisted
        const maxItemsRow = sharedPage.locator('.matrix-table__row').filter({hasText: 'max_items'});
        const maxItemsCell = maxItemsRow.locator('.matrix-table__cell').nth(2); // Second identifier column
        await expect(maxItemsCell.locator('.matrix-table__value')).toContainText('25');

        const featureFlagRow = sharedPage.locator('.matrix-table__row').filter({hasText: 'feature_flag'});
        const featureFlagCell = featureFlagRow.locator('.matrix-table__cell').nth(2); // Second identifier column
        await expect(featureFlagCell.locator('.matrix-table__value')).toContainText('true');
    });
});
