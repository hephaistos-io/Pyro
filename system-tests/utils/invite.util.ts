import {expect, Page} from '@playwright/test';
import {DEFAULT_PASSWORD} from './test-data.util';
import {getInviteLink} from './mailpit.util';

export type InviteRole = 'Admin' | 'Developer' | 'Viewer';

export interface CreateInviteOptions {
    /** Application names to grant access to */
    grantAccessToApps?: string[];
}

/**
 * Creates an invite via the UI and returns the invite URL from the email
 */
export async function createInvite(
    page: Page,
    email: string,
    role: InviteRole = 'Developer',
    options: CreateInviteOptions = {}
): Promise<string> {

    // Click the invite button
    await page.getByRole('button', {name: '+ Invite User'}).click();

    // Wait for the invite form
    await expect(page.getByLabel('Email')).toBeVisible();

    // Fill the form
    await page.getByLabel('Email Address').fill(email);

    // Select role using radio button
    await page.getByRole('radio', {name: new RegExp(role, 'i')}).click();

    // Select applications to grant access to
    if (options.grantAccessToApps && options.grantAccessToApps.length > 0) {
        for (const appName of options.grantAccessToApps) {
            await page.getByRole('checkbox', {name: appName}).check();
        }
    }

    // Submit the invite
    await page.getByRole('button', {name: 'Send Invitation'}).click();

    // Wait for success state
    await expect(page.getByText('Invitation Sent!')).toBeVisible();

    // Close the success overlay
    await page.getByRole('button', {name: 'Done'}).click();
    await expect(page.getByText('Invitation Sent!')).not.toBeVisible();

    // Get the invite URL from the email
    return await getInviteLink(email);
}

/**
 * Creates an invite without returning the URL (for simpler test cases)
 */
export async function createInviteSimple(
    page: Page,
    email: string,
    role: InviteRole = 'Developer'
): Promise<void> {
    // Click the invite button
    await page.getByRole('button', {name: '+ Invite User'}).click();

    // Wait for the invite form
    await expect(page.getByLabel('Email')).toBeVisible();

    // Fill the form
    await page.getByLabel('Email Address').fill(email);

    // Select role using radio button
    await page.getByRole('radio', {name: new RegExp(role, 'i')}).click();

    // Submit the invite
    await page.getByRole('button', {name: 'Send Invitation'}).click();

    // Wait for success state
    await expect(page.getByText('Invitation Sent!')).toBeVisible();

    // Close the success overlay
    await page.getByRole('button', {name: 'Done'}).click();
    await expect(page.getByText('Invitation Sent!')).not.toBeVisible();
}

/**
 * Completes registration via an invite link
 */
export async function completeInviteRegistration(
    page: Page,
    inviteUrl: string,
    firstName: string,
    lastName: string,
    password: string = DEFAULT_PASSWORD
): Promise<void> {
    // Navigate to a safe page first to clear auth state (register page redirects if logged in)
    await page.goto('/login');
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.clear());

    await page.goto(inviteUrl);

    // Wait for the invite banner (validation happens async)
    await expect(page.locator('.invite-banner')).toBeVisible();

    // Fill registration form
    await page.getByLabel('First Name').fill(firstName);
    await page.getByLabel('Last Name').fill(lastName);
    await page.getByLabel('Password', {exact: true}).fill(password);
    await page.getByLabel('Confirm Password').fill(password);

    // Submit registration (button text includes company name)
    await page.getByRole('button', {name: /Join/i}).click();

    // Wait for redirect to login
    await expect(page).toHaveURL('/login');
}

/**
 * Gets an invite URL by regenerating the invite from the users table
 * The URL is retrieved from the email sent by the backend
 */
export async function getInviteUrl(page: Page, invitedEmail: string): Promise<string> {
    const inviteRow = page.locator('.users-table__row').filter({hasText: invitedEmail});
    await inviteRow.locator('.btn-regenerate-user').click();

    // Wait for regenerate overlay to appear
    await expect(page.getByText('Invite Resent')).toBeVisible();

    // Close the overlay
    await page.getByRole('button', {name: 'Done'}).click();

    // Get the invite URL from the email
    return await getInviteLink(invitedEmail);
}
