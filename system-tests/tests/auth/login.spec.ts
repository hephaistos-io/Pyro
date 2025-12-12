import {expect, test} from '@playwright/test';

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
        await expect(page.locator('.error-message')).toBeVisible({timeout: 10000});
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
        // First register a new user with truly unique email
        const uniqueEmail = `login-test-${Date.now()}-${crypto.randomUUID()}@example.com`;
        const password = 'SecurePassword123!@#';

        await page.goto('/register');
        await page.getByLabel('First Name').fill('Test');
        await page.getByLabel('Last Name').fill('User');
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password', {exact: true}).fill(password);
        await page.getByLabel('Confirm Password').fill(password);
        await page.getByRole('button', {name: 'Create Account'}).click();

        // Wait for redirect to login page after registration
        await expect(page).toHaveURL('/login', {timeout: 15000});

        // Wait for the login form to be ready
        await expect(page.getByRole('heading', {name: 'Welcome back'})).toBeVisible();

        // Now log in with the newly created user
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password').fill(password);
        await page.getByRole('button', {name: 'Log In'}).click();

        // Verify redirect to dashboard
        await expect(page).toHaveURL('/dashboard', {timeout: 15000});
    });
});
