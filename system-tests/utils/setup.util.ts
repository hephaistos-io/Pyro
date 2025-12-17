import {Page} from '@playwright/test';
import {uniqueEmail, uniqueName} from './test-data.util';
import {loginUser, registerUser} from './auth.util';
import {createCompany} from './company.util';
import {createApplication, openApplication} from './application.util';

export interface SetupResult {
    email: string;
    appName: string;
    companyName: string;
}

/**
 * Sets up a complete user with company and application, then navigates to app overview.
 * Useful for tests that need a fully configured user.
 */
export async function setupUserWithApplication(page: Page): Promise<SetupResult> {
    const email = uniqueEmail('apikey-test');
    const companyName = uniqueName('TestCo');
    const appName = uniqueName('TestApp');

    // Register and login
    await registerUser(page, email, 'Test', 'User');
    await loginUser(page, email);

    // Create company
    await createCompany(page, companyName);

    // Create and open application
    await createApplication(page, appName);
    await openApplication(page, appName);

    return {email, appName, companyName};
}

/**
 * Sets up a user with a company but no application.
 */
export async function setupUserWithCompany(page: Page): Promise<{ email: string; companyName: string }> {
    const email = uniqueEmail('test');
    const companyName = uniqueName('TestCo');

    await registerUser(page, email, 'Test', 'User');
    await loginUser(page, email);
    await createCompany(page, companyName);

    return {email, companyName};
}
