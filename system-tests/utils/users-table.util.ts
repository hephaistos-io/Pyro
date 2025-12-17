import {expect, Page} from '@playwright/test';

export interface UserStatus {
    email: string;
    status: string;
}

/**
 * Gets all user emails displayed in the users table
 */
export async function getDisplayedUserEmails(page: Page): Promise<string[]> {
    // Wait for any loading to complete
    await expect(page.getByText('Loading users...')).not.toBeVisible({timeout: 10000});

    // Get all email elements
    const emailElements = page.locator('.user-identity__email');
    const count = await emailElements.count();

    const emails: string[] = [];
    for (let i = 0; i < count; i++) {
        const email = await emailElements.nth(i).textContent();
        if (email) {
            emails.push(email.trim());
        }
    }
    return emails;
}

/**
 * Gets user status badges from the table
 */
export async function getUserStatuses(page: Page): Promise<UserStatus[]> {
    // Wait for any loading to complete
    await expect(page.getByText('Loading users...')).not.toBeVisible({timeout: 10000});

    const rows = page.locator('.users-table__row');
    const count = await rows.count();

    const users: UserStatus[] = [];
    for (let i = 0; i < count; i++) {
        const row = rows.nth(i);
        const email = await row.locator('.user-identity__email').textContent();
        const statusBadge = row.locator('.status-badge');
        const status = await statusBadge.textContent();

        if (email && status) {
            users.push({
                email: email.trim(),
                status: status.trim().toLowerCase()
            });
        }
    }
    return users;
}

/**
 * Finds a user row by email
 */
export function getUserRow(page: Page, email: string) {
    return page.locator('.users-table__row').filter({hasText: email});
}
