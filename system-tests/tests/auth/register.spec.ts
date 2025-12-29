import {expect, test} from '@playwright/test';
import {getRegistrationVerificationLink} from '../../utils/mailpit.util';

test.describe('User Registration', () => {
    test.beforeEach(async ({page}) => {
        await page.goto('/register');
    });

    test('displays registration form', async ({page}) => {
        await expect(page.getByRole('heading', {name: 'Create your account'})).toBeVisible();
        await expect(page.getByLabel('First Name')).toBeVisible();
        await expect(page.getByLabel('Last Name')).toBeVisible();
        await expect(page.getByLabel('Email')).toBeVisible();
        await expect(page.getByLabel('Password', {exact: true})).toBeVisible();
        await expect(page.getByLabel('Confirm Password')).toBeVisible();
        await expect(page.getByRole('button', {name: 'Create Account'})).toBeVisible();
    });

    test('shows validation error when fields are empty', async ({page}) => {
        await page.getByRole('button', {name: 'Create Account'}).click();

        await expect(page.getByText('All fields are required')).toBeVisible();
    });

    test('shows error for invalid email format', async ({page}) => {
        await page.getByLabel('First Name').fill('John');
        await page.getByLabel('Last Name').fill('Doe');
        await page.getByLabel('Email').fill('invalid-email');
        await page.getByLabel('Password', {exact: true}).fill('SecurePassword123!');
        await page.getByLabel('Confirm Password').fill('SecurePassword123!');

        await page.getByRole('button', {name: 'Create Account'}).click();

        await expect(page.getByText('Please enter a valid email address')).toBeVisible();
    });

    test('shows error when passwords do not match', async ({page}) => {
        await page.getByLabel('First Name').fill('John');
        await page.getByLabel('Last Name').fill('Doe');
        await page.getByLabel('Email').fill('john@example.com');
        await page.getByLabel('Password', {exact: true}).fill('SecurePassword123!');
        await page.getByLabel('Confirm Password').fill('DifferentPassword123!');

        await page.getByRole('button', {name: 'Create Account'}).click();

        await expect(page.getByText('Passwords do not match')).toBeVisible();
    });

    test('shows password strength indicator', async ({page}) => {
        const passwordInput = page.getByLabel('Password', {exact: true});

        // Weak password
        await passwordInput.fill('weak');
        await expect(page.getByText('Too short')).toBeVisible();

        // Clear and try a stronger password
        await passwordInput.fill('SecurePassword123!@#');
        await expect(page.locator('.strength-label')).toBeVisible();
    });

    test('navigates to login page via link', async ({page}) => {
        await page.getByRole('link', {name: 'Log in'}).click();

        await expect(page).toHaveURL('/login');
        await expect(page.getByRole('heading', {name: 'Welcome back'})).toBeVisible();
    });

    test('successful registration shows email verification message', async ({page}) => {
        // Generate truly unique email to avoid conflicts in parallel tests
        const uniqueEmail = `test-${Date.now()}-${crypto.randomUUID()}@example.com`;

        await page.getByLabel('First Name').fill('Test');
        await page.getByLabel('Last Name').fill('User');
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password', {exact: true}).fill('SecurePassword123!@#');
        await page.getByLabel('Confirm Password').fill('SecurePassword123!@#');

        await page.getByRole('button', {name: 'Create Account'}).click();

        // Should show success message with email verification instructions
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();
        await expect(page.getByText('We\'ve sent a verification link to')).toBeVisible();
        await expect(page.getByText(uniqueEmail)).toBeVisible();

        // Should have a link to go to login
        await expect(page.getByRole('link', {name: 'Go to Login'})).toBeVisible();
    });

    test('unverified user cannot login', async ({page}) => {
        // Generate unique email for this test
        const uniqueEmail = `unverified-${Date.now()}-${crypto.randomUUID()}@example.com`;
        const password = 'SecurePassword123!@#';

        // Register
        await page.getByLabel('First Name').fill('Unverified');
        await page.getByLabel('Last Name').fill('User');
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password', {exact: true}).fill(password);
        await page.getByLabel('Confirm Password').fill(password);
        await page.getByRole('button', {name: 'Create Account'}).click();

        // Wait for success message
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();

        // Go to login
        await page.goto('/login');

        // Try to login
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password').fill(password);
        await page.getByRole('button', {name: 'Log In'}).click();

        // Should show email not verified error
        await expect(page.getByText('Please verify your email address before logging in')).toBeVisible();
    });

    test('complete email verification flow', async ({page}) => {
        // Generate unique email for this test
        const uniqueEmail = `verify-${Date.now()}-${crypto.randomUUID()}@example.com`;
        const password = 'SecurePassword123!@#';

        // Register
        await page.getByLabel('First Name').fill('Verified');
        await page.getByLabel('Last Name').fill('User');
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password', {exact: true}).fill(password);
        await page.getByLabel('Confirm Password').fill(password);
        await page.getByRole('button', {name: 'Create Account'}).click();

        // Wait for success message
        await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();

        // Get verification link from email
        const verificationUrl = await getRegistrationVerificationLink(uniqueEmail);

        // Visit the verification URL
        await page.goto(verificationUrl);

        // Should show success message
        await expect(page.getByRole('heading', {name: 'Email Verified!'})).toBeVisible();
        await expect(page.getByText('Your email has been verified successfully')).toBeVisible();

        // Click go to login
        await page.getByRole('button', {name: 'Go to Login'}).click();
        await expect(page).toHaveURL('/login');

        // Login should now work
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password').fill(password);
        await page.getByRole('button', {name: 'Log In'}).click();

        // Should be redirected to dashboard (company creation flow)
        await expect(page).toHaveURL(/\/(dashboard|register)/);
    });
});
