import {expect, Page} from '@playwright/test';
import {DEFAULT_PASSWORD} from './test-data.util';
import {getRegistrationVerificationLink} from './mailpit.util';

/**
 * Registers a new user via the registration form.
 * Completes the full registration flow including email verification.
 */
export async function registerUser(
    page: Page,
    email: string,
    firstName: string,
    lastName: string,
    password: string = DEFAULT_PASSWORD
): Promise<void> {
    await page.goto('/register');
    await page.getByLabel('First Name').fill(firstName);
    await page.getByLabel('Last Name').fill(lastName);
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', {exact: true}).fill(password);
    await page.getByLabel('Confirm Password').fill(password);
    await page.getByRole('button', {name: 'Create Account'}).click();

    // Wait for success message with email verification instructions
    await expect(page.getByRole('heading', {name: 'Check your email'})).toBeVisible();

    // Get verification link from email and verify
    const verificationUrl = await getRegistrationVerificationLink(email);
    await page.goto(verificationUrl);

    // Confirm email is verified
    await expect(page.getByRole('heading', {name: 'Email Verified!'})).toBeVisible();
}

/**
 * Logs in a user via the login form
 */
export async function loginUser(
    page: Page,
    email: string,
    password: string = DEFAULT_PASSWORD
): Promise<void> {
    await page.goto('/login');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', {name: 'Log In'}).click();

    await expect(page).toHaveURL('/dashboard');
}

/**
 * Registers a user and immediately logs them in
 */
export async function registerAndLogin(
    page: Page,
    email: string,
    firstName: string,
    lastName: string,
    password: string = DEFAULT_PASSWORD
): Promise<void> {
    await registerUser(page, email, firstName, lastName, password);
    await loginUser(page, email, password);
}
