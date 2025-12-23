import {expect, Page} from '@playwright/test';

/**
 * Navigates to the Template tab in the application overview
 */
export async function navigateToTemplateTab(page: Page): Promise<void> {
    await page.getByRole('button', {name: 'Template'}).click();
    await expect(page.locator('.template-tab')).toBeVisible();
}

/**
 * Navigates to the Overrides tab in the application overview
 */
export async function navigateToOverridesTab(page: Page): Promise<void> {
    await page.getByRole('button', {name: /Overrides/}).click();
    await expect(page.locator('.overrides-tab')).toBeVisible();
}

/**
 * Switches to System template type
 */
export async function selectSystemTemplateType(page: Page): Promise<void> {
    await page.locator('.template-toggle__btn').filter({hasText: 'System'}).click();
}

/**
 * Switches to User template type
 */
export async function selectUserTemplateType(page: Page): Promise<void> {
    await page.locator('.template-toggle__btn').filter({hasText: 'User'}).click();
}

/**
 * Adds a system template field
 */
export async function addSystemField(page: Page, key: string, value: string): Promise<void> {
    // Click Add Field button
    await page.getByRole('button', {name: 'Add Field'}).click();

    // Wait for overlay to appear
    await expect(page.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

    // Fill in key (labeled as "Field Key *")
    await page.getByLabel('Field Key *').fill(key);

    // Fill in default value for system fields
    await page.getByLabel('Default Value').fill(value);

    // Click Add Field button in the overlay (nth(1) because first one is the page button)
    await page.getByRole('button', {name: 'Add Field'}).nth(1).click();

    // Wait for overlay to close
    await expect(page.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();
    await page.waitForTimeout(500); // Wait for API call to complete
}

/**
 * Adds a user template field
 */
export async function addUserField(page: Page, key: string, type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'ENUM' = 'STRING'): Promise<void> {
    // Click Add Field button
    await page.getByRole('button', {name: 'Add Field'}).click();

    // Wait for overlay to appear
    await expect(page.getByRole('heading', {name: 'Add New Field'})).toBeVisible();

    // Fill in key
    await page.getByLabel('Field Key *').fill(key);

    // Select type
    await page.getByLabel('Type *').selectOption(type);

    // Click Add Field button in the overlay (nth(1) because first one is the page button)
    await page.getByRole('button', {name: 'Add Field'}).nth(1).click();

    // Wait for overlay to close
    await expect(page.getByRole('heading', {name: 'Add New Field'})).not.toBeVisible();
    await page.waitForTimeout(500); // Wait for API call to complete
}

/**
 * Deletes a template field by its key
 */
export async function deleteField(page: Page, fieldKey: string): Promise<void> {
    const row = page.locator('.template-matrix-table__row').filter({hasText: fieldKey});
    const deleteBtn = row.locator('.template-matrix-table__delete-btn');

    // Wait for button to be visible and enabled before clicking
    await expect(deleteBtn).toBeVisible();
    await deleteBtn.click();

    // Wait for delete confirmation overlay
    await expect(page.locator('.delete-field-overlay')).toBeVisible();

    // Get application name from the overlay hint
    const appNameHint = page.locator('.confirm-group').filter({hasText: 'Application Name'}).locator('.confirm-hint code');
    const appName = await appNameHint.textContent();

    // Fill in confirmation fields within the delete overlay
    const overlay = page.locator('.delete-field-overlay');
    const appNameInput = overlay.locator('input[placeholder="Enter application name"]');
    // Find the second input by looking for the confirm group containing "Field Name" or "Identifier Name"
    const fieldNameGroup = overlay.locator('.confirm-group').filter({hasText: /Field Name|Identifier Name/i});
    const fieldNameInput = fieldNameGroup.locator('input');

    if (appName) {
        await appNameInput.fill(appName);
    }
    await fieldNameInput.fill(fieldKey);

    // Wait for the delete button to be enabled (it's disabled until both fields match)
    // Scope to the overlay to avoid matching the table delete buttons
    const deleteButton = overlay.getByRole('button', {name: /Delete (Field|Identifier)/i});
    await expect(deleteButton).toBeEnabled();

    // Click delete button
    await deleteButton.click();

    // Wait for overlay to close
    await expect(page.locator('.delete-field-overlay')).not.toBeVisible();

    // Wait for the delete API call to complete
    await page.waitForTimeout(500);
}

/**
 * Gets all field keys from the current template table
 */
export async function getFieldKeys(page: Page): Promise<string[]> {
    const fieldCodes = page.locator('.template-matrix-table__field-name');
    const count = await fieldCodes.count();
    const keys: string[] = [];
    for (let i = 0; i < count; i++) {
        const text = await fieldCodes.nth(i).textContent();
        if (text) keys.push(text.trim());
    }
    return keys;
}

/**
 * Checks if a field exists in the template table
 */
export async function fieldExists(page: Page, fieldKey: string): Promise<boolean> {
    const keys = await getFieldKeys(page);
    return keys.includes(fieldKey);
}

/**
 * Edits a cell in the template table
 */
export async function editTemplateCell(page: Page, fieldKey: string, column: 'key' | 'value' | 'description', newValue: string): Promise<void> {
    const row = page.locator('.template-matrix-table__row').filter({hasText: fieldKey});

    // Find the correct cell based on column
    let cell;
    if (column === 'key') {
        cell = row.locator('.template-matrix-table__field-name');
    } else if (column === 'value') {
        cell = row.locator('.template-matrix-table__value').first();
    } else {
        cell = row.locator('.template-matrix-table__description');
    }

    // Click to edit
    await cell.click();

    // Wait for input to appear and fill it
    const input = row.locator('.template-matrix-table__input');
    await expect(input).toBeVisible();
    await input.clear();
    await input.fill(newValue);
    await input.press('Enter');

    // Wait for edit mode to close and API to complete
    await expect(input).not.toBeVisible();
    await page.waitForTimeout(500); // Wait for API call to complete
}

/**
 * Adds an identifier in the Overrides tab
 */
export async function addIdentifier(page: Page, identifier: string): Promise<void> {
    // Click Add Identifier button
    await page.getByRole('button', {name: 'Add Identifier'}).click();

    // Wait for form to appear
    await expect(page.locator('.overrides-add-form')).toBeVisible();

    // Fill in identifier
    await page.locator('.overrides-add-form__input').fill(identifier);

    // Click Add
    await page.locator('.overrides-add-form__btn--save').click();

    // Wait for form to close
    await expect(page.locator('.overrides-add-form')).not.toBeVisible();

    // Wait for identifier to appear in table headers
    await expect(page.locator('.matrix-table__identifier-header', {hasText: identifier})).toBeVisible();
}

/**
 * Gets all identifiers from the overrides matrix table header
 */
export async function getIdentifiers(page: Page): Promise<string[]> {
    const identifierHeaders = page.locator('.matrix-table__identifier-header');
    const count = await identifierHeaders.count();
    const identifiers: string[] = [];
    for (let i = 0; i < count; i++) {
        const text = await identifierHeaders.nth(i).textContent();
        if (text) identifiers.push(text.trim());
    }
    return identifiers;
}

/**
 * Edits an override value in the matrix table
 */
export async function editOverrideValue(page: Page, fieldKey: string, identifier: string, newValue: string): Promise<void> {
    // Find the row by field key
    const row = page.locator('.matrix-table__row').filter({
        has: page.locator('.matrix-table__field-name', {hasText: fieldKey})
    });

    // Get the column index for the identifier
    const headers = page.locator('.matrix-table__identifier-header');
    const headerCount = await headers.count();
    let columnIndex = -1;
    for (let i = 0; i < headerCount; i++) {
        const text = await headers.nth(i).textContent();
        if (text?.trim() === identifier) {
            columnIndex = i + 1; // +1 because first column is the field name
            break;
        }
    }

    if (columnIndex === -1) {
        throw new Error(`Identifier "${identifier}" not found in table headers`);
    }

    // Click the cell to edit
    const cell = row.locator('.matrix-table__cell').nth(columnIndex);
    await cell.locator('.matrix-table__value').click();

    // Fill in the new value
    const input = cell.locator('.matrix-table__input');
    await expect(input).toBeVisible();

    // Check if it's a select element or input element
    const tagName = await input.evaluate(el => el.tagName.toLowerCase());
    if (tagName === 'select') {
        await input.selectOption(newValue);
    } else {
        await input.fill(newValue);
    }
    await input.press('Enter');

    // Wait for edit mode to close
    await expect(input).not.toBeVisible();
}

/**
 * Gets the column index for a specific identifier in the overrides matrix table.
 * Returns the column index (1-based, accounting for the field name column) or -1 if not found.
 */
export async function getIdentifierColumnIndex(page: Page, identifier: string): Promise<number> {
    const headers = page.locator('.matrix-table__identifier-header');
    const headerCount = await headers.count();
    for (let i = 0; i < headerCount; i++) {
        const text = await headers.nth(i).textContent();
        if (text?.trim() === identifier) {
            return i + 1; // +1 because first column is the field name
        }
    }
    return -1;
}

/**
 * Deletes an identifier from the overrides matrix
 */
export async function deleteIdentifier(page: Page, identifier: string): Promise<void> {
    // Find the identifier column and click its delete button
    const headers = page.locator('.matrix-table__identifier-header');
    const headerCount = await headers.count();
    for (let i = 0; i < headerCount; i++) {
        const text = await headers.nth(i).textContent();
        if (text?.trim() === identifier) {
            // Click the delete button for this column
            await page.locator('.matrix-table__identifier-delete').nth(i).click();
            break;
        }
    }

    // Wait for delete confirmation overlay
    await expect(page.locator('.delete-field-overlay')).toBeVisible();

    // Get application name from the overlay hint
    const appNameHint = page.locator('.confirm-group').filter({hasText: 'Application Name'}).locator('.confirm-hint code');
    const appName = await appNameHint.textContent();

    // Fill in confirmation fields within the delete overlay
    const overlay = page.locator('.delete-field-overlay');
    const appNameInput = overlay.locator('input[placeholder="Enter application name"]');
    const identifierNameGroup = overlay.locator('.confirm-group').filter({hasText: 'Identifier Name'});
    const identifierNameInput = identifierNameGroup.locator('input');

    if (appName) {
        await appNameInput.fill(appName);
    }
    await identifierNameInput.fill(identifier);

    // Wait for the delete button to be enabled (it's disabled until both fields match)
    // Scope to the overlay to avoid matching other delete buttons
    const deleteButton = overlay.getByRole('button', {name: 'Delete Identifier'});
    await expect(deleteButton).toBeEnabled();

    // Click delete button
    await deleteButton.click();

    // Wait for overlay to close
    await expect(page.locator('.delete-field-overlay')).not.toBeVisible();
}
