import {expect, test} from '@playwright/test';
import {loginUser, registerUser} from '../../utils';

test.describe('User Login', () => {
    test.beforeEach(async ({page}) => {
        await page.goto('/login');
    });

    test('displays login form', async ({page}) => {
        await expect(page.getByRole('heading', {name: 'Welcome back'})).toBeVisible();
        await expect(page.getByLabel('Email')).toBeVisible();
        await expect(page.getByLabel('Password')).toBeVisible();
        await expect(page.getByRole('button', {name: 'Log In'})).toBeVisible();
    });

    test('shows validation error when fields are empty', async ({page}) => {
        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page.getByText('Email and password are required')).toBeVisible();
    });

    test('shows error for invalid email format', async ({page}) => {
        await page.getByLabel('Email').fill('invalid-email');
        await page.getByLabel('Password').fill('somepassword');

        await page.getByRole('button', {name: 'Log In'}).click();

        await expect(page.getByText('Please enter a valid email address')).toBeVisible();
    });

    test('shows error for invalid credentials', async ({page}) => {
        await page.getByLabel('Email').fill('nonexistent@example.com');
        await page.getByLabel('Password').fill('wrongpassword');

        await page.getByRole('button', {name: 'Log In'}).click();

        // Wait for API response and error message
        await expect(page.locator('.error-message')).toBeVisible();
    });

    test('navigates to register page via link', async ({page}) => {
        await page.getByRole('link', {name: 'Sign up'}).click();

        await expect(page).toHaveURL('/register');
        await expect(page.getByRole('heading', {name: 'Create your account'})).toBeVisible();
    });

    test('shows loading state during login attempt', async ({page}) => {
        await page.getByLabel('Email').fill('test@example.com');
        await page.getByLabel('Password').fill('somepassword');

        // Click and immediately check for loading state
        await page.getByRole('button', {name: 'Log In'}).click();

        // Button should show loading state (spinner or text change)
        await expect(page.getByText('Logging in...')).toBeVisible();
    });

    test('successful login redirects to dashboard', async ({page}) => {
        // Register a new user (includes email verification)
        const uniqueEmail = `login-test-${Date.now()}-${crypto.randomUUID()}@example.com`;
        const password = 'SecurePassword123!@#';

        await registerUser(page, uniqueEmail, 'Test', 'User', password);

        // Now log in with the newly created and verified user
        await loginUser(page, uniqueEmail, password);

        // Verify we're on the dashboard
        await expect(page).toHaveURL('/dashboard');
    });
});
