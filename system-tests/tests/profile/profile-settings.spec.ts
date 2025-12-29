import {expect, test} from '@playwright/test';
import {DEFAULT_PASSWORD, uniqueEmail} from '../../utils/test-data.util';
import {setupUserWithCompany} from '../../utils/setup.util';
import {
    completeEmailChange,
    completePasswordReset,
    goToProfileSettings,
    requestEmailChangeFromProfile,
    requestPasswordResetFromProfile,
    updateProfile
} from '../../utils/profile.util';

/**
 * Profile tests use setupUserWithCompany to bypass the onboarding overlay.
 * New users see an onboarding overlay until they create a company.
 */
test.describe('Profile Settings Page', () => {
    test('displays profile settings when logged in', async ({page}) => {
        await setupUserWithCompany(page);

        await goToProfileSettings(page);

        await expect(page.getByRole('heading', {name: 'Profile Settings'})).toBeVisible();
        await expect(page.getByRole('heading', {name: 'Profile Information'})).toBeVisible();
        await expect(page.getByRole('heading', {name: 'Email Address'})).toBeVisible();
        await expect(page.getByRole('heading', {name: 'Password'})).toBeVisible();
    });

    test('shows current user data in profile form', async ({page}) => {
        const {email} = await setupUserWithCompany(page);

        await goToProfileSettings(page);

        // First name is "Test" and last name is "User" from setupUserWithCompany
        await expect(page.getByLabel('First Name')).toHaveValue('Test');
        await expect(page.getByLabel('Last Name')).toHaveValue('User');
        await expect(page.locator('.current-email')).toContainText(email);
    });

    test('has back to dashboard link', async ({page}) => {
        await setupUserWithCompany(page);

        await goToProfileSettings(page);

        // Navigate using the link's href directly (navbar overlays the click target)
        const backLink = page.locator('.back-link');
        await expect(backLink).toBeVisible();
        const href = await backLink.getAttribute('href');
        expect(href).toBe('/dashboard');
        await page.goto(href!);
        await expect(page).toHaveURL('/dashboard');
    });
});

test.describe('Profile Information Update', () => {
    test('can update first and last name', async ({page}) => {
        await setupUserWithCompany(page);

        await goToProfileSettings(page);

        await updateProfile(page, 'Updated', 'NewName');

        // Verify values are saved
        await expect(page.getByLabel('First Name')).toHaveValue('Updated');
        await expect(page.getByLabel('Last Name')).toHaveValue('NewName');
    });

    test('profile updates persist after page reload', async ({page}) => {
        await setupUserWithCompany(page);

        await goToProfileSettings(page);
        await updateProfile(page, 'After', 'Change');

        // Reload and verify
        await page.reload();
        await expect(page.getByRole('heading', {name: 'Profile Settings'})).toBeVisible();
        await expect(page.getByLabel('First Name')).toHaveValue('After');
        await expect(page.getByLabel('Last Name')).toHaveValue('Change');
    });
});

test.describe('Password Reset from Profile (Logged In)', () => {
    test('can request password reset from profile page', async ({page}) => {
        const {email} = await setupUserWithCompany(page);

        await goToProfileSettings(page);

        // Request reset and get URL from email
        const resetUrl = await requestPasswordResetFromProfile(page, email);

        expect(resetUrl).toBeTruthy();
        expect(resetUrl).toContain('/reset-password?token=');
    });

    test('complete password reset flow from profile', async ({page}) => {
        const {email} = await setupUserWithCompany(page);
        const newPassword = 'ProfileNewPw123!@#';

        await goToProfileSettings(page);

        // Get reset URL from email
        const resetUrl = await requestPasswordResetFromProfile(page, email);

        // Clear auth state (simulating opening link in new session)
        await page.context().clearCookies();
        await page.evaluate(() => localStorage.clear());

        // Complete the reset
        await completePasswordReset(page, resetUrl, newPassword);

        // Navigate to login page
        await page.goto('/login');
        await expect(page.getByRole('heading', {name: 'Welcome back'})).toBeVisible();

        // Login with new password
        await page.getByLabel('Email').fill(email);
        await page.getByLabel('Password').fill(newPassword);
        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page).toHaveURL('/dashboard');
    });

    test('existing session invalidated after password reset', async ({page, browser}) => {
        const {email} = await setupUserWithCompany(page);

        await goToProfileSettings(page);
        const resetUrl = await requestPasswordResetFromProfile(page, email);

        // Open reset URL in a NEW browser context (no shared cookies/localStorage)
        const newContext = await browser.newContext();
        const newPage = await newContext.newPage();
        await newPage.goto(resetUrl);
        await expect(newPage.getByRole('heading', {name: 'Reset your password'})).toBeVisible();

        // Use same password pattern that works in other tests
        const newPassword = 'SessionInvalidate123!@#';
        await newPage.getByLabel('New Password').fill(newPassword);
        await newPage.getByLabel('Confirm Password').fill(newPassword);
        await newPage.getByRole('button', {name: 'Reset Password'}).click();

        // Wait for reset to complete
        await expect(newPage.getByRole('heading', {name: 'Password Reset Complete'})).toBeVisible();
        await newContext.close();

        // Try to use original page - should be logged out/redirected
        await page.goto('/dashboard/profile');

        // Should be redirected to login (JWT invalidated by passwordChangedAt)
        // URL may include returnUrl query param
        await expect(page).toHaveURL(/\/login/);
    });
});

test.describe('Email Change from Profile', () => {
    test('can request email change from profile page', async ({page}) => {
        const newEmail = uniqueEmail('newemail');

        await setupUserWithCompany(page);
        await goToProfileSettings(page);

        const verificationUrl = await requestEmailChangeFromProfile(page, newEmail);

        expect(verificationUrl).toBeTruthy();
        expect(verificationUrl).toContain('/verify-email?token=');
    });

    test('complete email change flow', async ({page}) => {
        const newEmail = uniqueEmail('changedmail');

        await setupUserWithCompany(page);
        await goToProfileSettings(page);

        // Request email change and get verification URL
        const verificationUrl = await requestEmailChangeFromProfile(page, newEmail);

        // Complete verification (clears auth state to simulate new session)
        await completeEmailChange(page, verificationUrl);

        // Login with new email
        await page.getByRole('button', {name: 'Log In'}).click();
        await page.getByLabel('Email').fill(newEmail);
        await page.getByLabel('Password').fill(DEFAULT_PASSWORD);
        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page).toHaveURL('/dashboard');

        // Verify profile shows new email
        await goToProfileSettings(page);
        await expect(page.locator('.current-email')).toContainText(newEmail);
    });

    test('old email no longer works after change', async ({page}) => {
        const {email: originalEmail} = await setupUserWithCompany(page);
        const newEmail = uniqueEmail('replaceemail');

        await goToProfileSettings(page);

        const verificationUrl = await requestEmailChangeFromProfile(page, newEmail);
        await completeEmailChange(page, verificationUrl);

        // Try to login with old email
        await page.goto('/login');
        await page.getByLabel('Email').fill(originalEmail);
        await page.getByLabel('Password').fill(DEFAULT_PASSWORD);
        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page.locator('.error-message')).toBeVisible();
        await expect(page).toHaveURL('/login');
    });

    test('email verification token can only be used once', async ({page}) => {
        const newEmail = uniqueEmail('singleuse');

        await setupUserWithCompany(page);
        await goToProfileSettings(page);

        const verificationUrl = await requestEmailChangeFromProfile(page, newEmail);

        // Use the token once
        await completeEmailChange(page, verificationUrl);

        // Try to use it again
        await page.goto(verificationUrl);

        await expect(page.getByRole('heading', {name: 'Invalid or Expired Link'})).toBeVisible();
    });
});

test.describe('Email Verification Page', () => {
    test('shows error for invalid token', async ({page}) => {
        await page.goto('/verify-email?token=invalid-token-abc');

        await expect(page.getByRole('heading', {name: 'Invalid or Expired Link'})).toBeVisible();
    });

    test('shows error when no token provided', async ({page}) => {
        await page.goto('/verify-email');

        await expect(page.getByRole('heading', {name: 'Invalid or Expired Link'})).toBeVisible();
    });

    test('shows pending email in confirmation view', async ({page}) => {
        const newEmail = uniqueEmail('pending');

        await setupUserWithCompany(page);
        await goToProfileSettings(page);

        const verificationUrl = await requestEmailChangeFromProfile(page, newEmail);

        // Navigate without completing - should show confirmation UI
        await page.context().clearCookies();
        await page.evaluate(() => localStorage.clear());
        await page.goto(verificationUrl);

        await expect(page.getByRole('heading', {name: 'Confirm Email Change'})).toBeVisible();
        await expect(page.getByText(newEmail)).toBeVisible();
    });
});

test.describe('Profile Page Navigation', () => {
    test('can access profile via navbar icon', async ({page}) => {
        await setupUserWithCompany(page);

        // Wait for dashboard to load then click profile icon
        await expect(page.locator('.profile-link')).toBeVisible();
        await page.locator('.profile-link').click();

        await expect(page).toHaveURL('/dashboard/profile');
        await expect(page.getByRole('heading', {name: 'Profile Settings'})).toBeVisible();
    });

    test('redirects to login when not authenticated', async ({page}) => {
        await page.goto('/dashboard/profile');

        // URL may include returnUrl query param
        await expect(page).toHaveURL(/\/login/);
    });
});
