import {expect, test} from '@playwright/test';

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

    test('successful registration redirects to login', async ({page}) => {
        // Generate truly unique email to avoid conflicts in parallel tests
        const uniqueEmail = `test-${Date.now()}-${crypto.randomUUID()}@example.com`;

        await page.getByLabel('First Name').fill('Test');
        await page.getByLabel('Last Name').fill('User');
        await page.getByLabel('Email').fill(uniqueEmail);
        await page.getByLabel('Password', {exact: true}).fill('SecurePassword123!@#');
        await page.getByLabel('Confirm Password').fill('SecurePassword123!@#');

        await page.getByRole('button', {name: 'Create Account'}).click();

        // Wait for redirect to login page
        await expect(page).toHaveURL('/login', {timeout: 10000});
    });
});
