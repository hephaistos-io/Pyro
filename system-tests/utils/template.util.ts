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

    // Wait for inline form to appear (system form has 2 inputs, no select)
    const form = page.locator('.template-add-form-inline').filter({hasNot: page.locator('.template-add-form-inline__select')});
    await expect(form).toBeVisible();

    // Fill in key and value
    await form.locator('.template-add-form-inline__input').first().fill(key);
    await form.locator('.template-add-form-inline__input').last().fill(value);

    // Click Add
    await form.locator('.template-add-form-inline__save').click();

    // Wait for form to close and API to complete
    await expect(form).not.toBeVisible();
    await page.waitForTimeout(500); // Wait for API call to complete
}

/**
 * Adds a user template field
 */
export async function addUserField(page: Page, key: string, type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'ENUM' = 'STRING'): Promise<void> {
    // Click Add Field button
    await page.getByRole('button', {name: 'Add Field'}).click();

    // Wait for inline form to appear - look for the form within the user template section
    const form = page.locator('.template-add-form-inline').filter({has: page.locator('.template-add-form-inline__select')});
    await expect(form).toBeVisible();

    // Fill in key
    await form.locator('.template-add-form-inline__input').fill(key);

    // Select type
    await form.locator('.template-add-form-inline__select').selectOption(type);

    // Click Add
    await form.locator('.template-add-form-inline__save').click();

    // Wait for form to close and field to appear in table
    await expect(form).not.toBeVisible();
    await page.waitForTimeout(500); // Wait for API call to complete
}

/**
 * Deletes a template field by its key
 */
export async function deleteField(page: Page, fieldKey: string): Promise<void> {
    const row = page.locator('.template-matrix-table__row').filter({hasText: fieldKey});
    await row.locator('.template-matrix-table__delete-btn').click();
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
    await input.fill(newValue);
    await input.press('Enter');

    // Wait for edit mode to close
    await expect(input).not.toBeVisible();
}
