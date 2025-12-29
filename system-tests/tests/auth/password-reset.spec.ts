import {expect, test} from '@playwright/test';
import {clearMailbox, getPasswordResetLink} from '../../utils/mailpit.util';
import {registerUser} from '../../utils/auth.util';
import {uniqueEmail} from '../../utils/test-data.util';

/**
 * These tests cover the public "Forgot Password" flow from the login page.
 * Full flow tests now use Mailpit to extract reset URLs from actual emails.
 */
test.describe('Password Reset - Forgot Password Flow (Login Page)', () => {
    test.beforeEach(async ({page}) => {
        await page.goto('/forgot-password');
    });

    test('displays forgot password form', async ({page}) => {
        await expect(page.getByRole('heading', {name: 'Forgot your password?'})).toBeVisible();
        await expect(page.getByLabel('Email')).toBeVisible();
        await expect(page.getByRole('button', {name: 'Send Reset Link'})).toBeVisible();
    });

    test('shows success message after submitting email', async ({page}) => {
        await page.getByLabel('Email').fill('test@example.com');
        await page.getByRole('button', {name: 'Send Reset Link'}).click();

        // Wait for loading to complete and success to show
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();
        await expect(page.getByText('If an account exists')).toBeVisible();
    });

    test('shows success even for non-existent email (no account enumeration)', async ({page}) => {
        await page.getByLabel('Email').fill('nonexistent-user@example.com');
        await page.getByRole('button', {name: 'Send Reset Link'}).click();

        // Should still show success (no account enumeration)
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();
    });

    test('has link back to login page', async ({page}) => {
        await page.getByRole('link', {name: 'Log in'}).click();
        await expect(page).toHaveURL('/login');
    });

    test('login page has forgot password link', async ({page}) => {
        await page.goto('/login');
        await expect(page.getByRole('link', {name: 'Forgot password?'})).toBeVisible();

        await page.getByRole('link', {name: 'Forgot password?'}).click();
        await expect(page).toHaveURL('/forgot-password');
    });

    test('shows validation error for empty email', async ({page}) => {
        await page.getByRole('button', {name: 'Send Reset Link'}).click();
        await expect(page.getByText('Email is required')).toBeVisible();
    });

    test('shows validation error for invalid email format', async ({page}) => {
        await page.getByLabel('Email').fill('not-an-email');
        await page.getByRole('button', {name: 'Send Reset Link'}).click();
        await expect(page.getByText('valid email')).toBeVisible();
    });
});

test.describe('Password Reset - Reset Password Page', () => {
    test('shows error for invalid token', async ({page}) => {
        await page.goto('/reset-password?token=invalid-token-123');

        await expect(page.getByRole('heading', {name: 'Invalid or Expired Link'})).toBeVisible();
        await expect(page.getByRole('link', {name: 'Request New Link'})).toBeVisible();
    });

    test('shows error when no token provided', async ({page}) => {
        await page.goto('/reset-password');

        await expect(page.getByRole('heading', {name: 'Invalid or Expired Link'})).toBeVisible();
    });
});

test.describe('Password Reset - Full Flow via Email', () => {
    test('complete forgot password flow via email', async ({page}) => {
        // Create a user first
        const email = uniqueEmail('forgot-pw');
        await registerUser(page, email, 'Test', 'User');

        // Clear mailbox to ensure we get the right email
        await clearMailbox();

        // Request password reset via forgot password page
        await page.goto('/forgot-password');
        await page.getByLabel('Email').fill(email);
        await page.getByRole('button', {name: 'Send Reset Link'}).click();

        // Wait for success message
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();

        // Get reset URL from email via Mailpit
        const resetUrl = await getPasswordResetLink(email);
        expect(resetUrl).toContain('/reset-password?token=');

        // Navigate to reset URL and complete reset
        await page.goto(resetUrl);
        await expect(page.getByRole('heading', {name: 'Reset your password'})).toBeVisible();

        const newPassword = 'ForgotPwFlow123!@#';
        await page.getByLabel('New Password').fill(newPassword);
        await page.getByLabel('Confirm Password').fill(newPassword);
        await page.getByRole('button', {name: 'Reset Password'}).click();

        // Wait for success
        await expect(page.getByRole('heading', {name: 'Password Reset Complete'})).toBeVisible();

        // Login with new password
        await page.goto('/login');
        await page.getByLabel('Email').fill(email);
        await page.getByLabel('Password').fill(newPassword);
        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page).toHaveURL('/dashboard');
    });
});
