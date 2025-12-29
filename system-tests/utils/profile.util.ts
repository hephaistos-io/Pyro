import {expect, Page} from '@playwright/test';
import {getEmailVerificationLink, getPasswordResetLink} from './mailpit.util';

/**
 * Navigates to profile settings page (requires logged in user)
 */
export async function goToProfileSettings(page: Page): Promise<void> {
    await page.goto('/dashboard/profile');
    await expect(page.getByRole('heading', {name: 'Profile Settings'})).toBeVisible();
}

/**
 * Gets the current user's email from the profile page
 */
export async function getCurrentUserEmail(page: Page): Promise<string> {
    const emailElement = page.locator('.current-email');
    const emailText = await emailElement.textContent();
    if (!emailText) {
        throw new Error('Could not find current user email on profile page');
    }
    return emailText.trim();
}

/**
 * Updates profile information (first name, last name)
 */
export async function updateProfile(
    page: Page,
    firstName: string,
    lastName: string
): Promise<void> {
    await page.getByLabel('First Name').fill(firstName);
    await page.getByLabel('Last Name').fill(lastName);
    await page.getByRole('button', {name: 'Save Changes'}).click();

    await expect(page.getByText('Profile updated successfully')).toBeVisible();
}

/**
 * Requests a password reset from profile page and returns the reset URL from email.
 * Uses Mailpit to retrieve the reset URL from the sent email.
 * @param email - The user's email address (used to find the email in Mailpit)
 */
export async function requestPasswordResetFromProfile(page: Page, email: string): Promise<string> {
    await page.getByRole('button', {name: 'Reset Password'}).click();

    // Wait for success message indicating email was sent
    await expect(page.getByText('Password reset email sent')).toBeVisible();

    // Get the reset URL from Mailpit
    return await getPasswordResetLink(email);
}

/**
 * Requests an email change from profile page and returns the verification URL from email.
 * Uses Mailpit to retrieve the verification URL from the sent email.
 * @param newEmail - The new email address to change to
 */
export async function requestEmailChangeFromProfile(
    page: Page,
    newEmail: string
): Promise<string> {
    await page.getByLabel('New Email Address').fill(newEmail);
    await page.getByRole('button', {name: 'Change Email'}).click();

    // Wait for success message indicating email was sent
    await expect(page.getByText('Verification email sent')).toBeVisible();

    // Get the verification URL from Mailpit (email is sent to the new address)
    return await getEmailVerificationLink(newEmail);
}

/**
 * Completes password reset by navigating to reset URL and entering new password
 */
export async function completePasswordReset(
    page: Page,
    resetUrl: string,
    newPassword: string
): Promise<void> {
    await page.goto(resetUrl);

    // Wait for token validation
    await expect(page.getByRole('heading', {name: 'Reset your password'})).toBeVisible();

    // Fill in new password
    await page.getByLabel('New Password').fill(newPassword);
    await page.getByLabel('Confirm Password').fill(newPassword);
    await page.getByRole('button', {name: 'Reset Password'}).click();

    // Wait for success
    await expect(page.getByRole('heading', {name: 'Password Reset Complete'})).toBeVisible();
}

/**
 * Completes email change by navigating to verification URL and confirming
 */
export async function completeEmailChange(
    page: Page,
    verificationUrl: string
): Promise<void> {
    // Clear auth state so we can verify as if coming from email
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.clear());

    await page.goto(verificationUrl);

    // Wait for token validation
    await expect(page.getByRole('heading', {name: 'Confirm Email Change'})).toBeVisible();

    // Confirm the change
    await page.getByRole('button', {name: 'Confirm Email Change'}).click();

    // Wait for success
    await expect(page.getByText('Email Updated')).toBeVisible();
}
