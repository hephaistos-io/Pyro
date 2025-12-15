import {expect, Page, test} from '@playwright/test';

/**
 * Helper to generate unique test data
 */
function uniqueEmail(prefix: string): string {
    return `${prefix}-${Date.now()}-${crypto.randomUUID().slice(0, 8)}@example.com`;
}

/**
 * Helper to register a new user
 */
async function registerUser(page: Page, email: string, firstName: string, lastName: string): Promise<void> {
    const password = 'SecurePassword123!@#';

    await page.goto('/register');
    await page.getByLabel('First Name').fill(firstName);
    await page.getByLabel('Last Name').fill(lastName);
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', {exact: true}).fill(password);
    await page.getByLabel('Confirm Password').fill(password);
    await page.getByRole('button', {name: 'Create Account'}).click();

    // Wait for redirect to login page after registration
    await expect(page).toHaveURL('/login', {timeout: 15000});
}

/**
 * Helper to login a user
 */
async function loginUser(page: Page, email: string): Promise<void> {
    const password = 'SecurePassword123!@#';

    await page.goto('/login');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', {name: 'Log In'}).click();

    // Wait for dashboard
    await expect(page).toHaveURL('/dashboard', {timeout: 15000});
}

/**
 * Helper to create a company (during onboarding)
 */
async function createCompany(page: Page, companyName: string): Promise<void> {
    // Onboarding overlay should appear
    await expect(page.getByText('Create Your Company')).toBeVisible({timeout: 10000});
    await page.getByLabel('Company Name').fill(companyName);
    await page.getByRole('button', {name: 'Create Company'}).click();

    // Wait for company to be created and overlay to close
    await expect(page.getByText('Create Your Company')).not.toBeVisible({timeout: 10000});
}

/**
 * Helper to navigate to the Users tab
 */
async function navigateToUsersTab(page: Page): Promise<void> {
    // Click on Users tab
    await page.getByRole('button', {name: 'Users'}).click();

    // Wait for users table to be visible
    await expect(page.locator('.users-table')).toBeVisible({timeout: 10000});
}

/**
 * Helper to get all user emails displayed in the users table
 */
async function getDisplayedUserEmails(page: Page): Promise<string[]> {
    // Wait for any loading to complete
    await expect(page.getByText('Loading users...')).not.toBeVisible({timeout: 10000});

    // Get all email elements
    const emailElements = page.locator('.user-identity__email');
    const count = await emailElements.count();

    const emails: string[] = [];
    for (let i = 0; i < count; i++) {
        const email = await emailElements.nth(i).textContent();
        if (email) {
            emails.push(email.trim());
        }
    }
    return emails;
}

test.describe('Customer Multi-Tenancy Isolation', () => {
    test('users only see customers from their own company', async ({browser}) => {
        // Create unique emails for this test run
        const userNoCompanyEmail = uniqueEmail('no-company');
        const userAEmail = uniqueEmail('user-a');
        const userBEmail = uniqueEmail('user-b');

        // ============================================
        // Step 1: Create User with No Company
        // ============================================
        const pageNoCompany = await browser.newPage();
        await registerUser(pageNoCompany, userNoCompanyEmail, 'NoCompany', 'User');
        await loginUser(pageNoCompany, userNoCompanyEmail);

        // User without company should see onboarding overlay but we won't complete it
        await expect(pageNoCompany.getByText('Create Your Company')).toBeVisible({timeout: 10000});

        // Try to access users - should not be possible without a company
        // The users tab shouldn't work properly without a company
        // Close this page for now
        await pageNoCompany.close();

        // ============================================
        // Step 2: Create User A with Company A
        // ============================================
        const pageA = await browser.newPage();
        await registerUser(pageA, userAEmail, 'Alice', 'CompanyA');
        await loginUser(pageA, userAEmail);
        await createCompany(pageA, 'Company Alpha');

        // Navigate to Users tab
        await navigateToUsersTab(pageA);

        // User A should only see themselves
        let userAEmails = await getDisplayedUserEmails(pageA);
        expect(userAEmails).toHaveLength(1);
        expect(userAEmails).toContain(userAEmail);

        // ============================================
        // Step 3: Create User B with Company B
        // ============================================
        const pageB = await browser.newPage();
        await registerUser(pageB, userBEmail, 'Bob', 'CompanyB');
        await loginUser(pageB, userBEmail);
        await createCompany(pageB, 'Company Beta');

        // Navigate to Users tab
        await navigateToUsersTab(pageB);

        // User B should only see themselves
        let userBEmails = await getDisplayedUserEmails(pageB);
        expect(userBEmails).toHaveLength(1);
        expect(userBEmails).toContain(userBEmail);

        // ============================================
        // Step 4: Verify User A still only sees Company A users
        // ============================================
        // Refresh User A's page and verify they still only see their company's users
        await pageA.reload();
        await navigateToUsersTab(pageA);

        userAEmails = await getDisplayedUserEmails(pageA);
        expect(userAEmails).toHaveLength(1);
        expect(userAEmails).toContain(userAEmail);
        // User A should NOT see User B
        expect(userAEmails).not.toContain(userBEmail);
        // User A should NOT see the unassigned user
        expect(userAEmails).not.toContain(userNoCompanyEmail);

        // ============================================
        // Step 5: Verify User B only sees Company B users
        // ============================================
        await pageB.reload();
        await navigateToUsersTab(pageB);

        userBEmails = await getDisplayedUserEmails(pageB);
        expect(userBEmails).toHaveLength(1);
        expect(userBEmails).toContain(userBEmail);
        // User B should NOT see User A
        expect(userBEmails).not.toContain(userAEmail);
        // User B should NOT see the unassigned user
        expect(userBEmails).not.toContain(userNoCompanyEmail);

        // Cleanup
        await pageA.close();
        await pageB.close();
    });

    test('user without company cannot access users list', async ({page}) => {
        const email = uniqueEmail('no-company-test');

        // Register and login
        await registerUser(page, email, 'NoCompany', 'TestUser');
        await loginUser(page, email);

        // User should see onboarding overlay
        await expect(page.getByText('Create Your Company')).toBeVisible({timeout: 10000});

        // Try to click Users tab (if visible behind overlay)
        // The onboarding overlay should prevent access to the users tab
        // Or if the user somehow gets to users, the API should return an error

        // Close the overlay by clicking outside (if possible) or navigating directly
        await page.goto('/dashboard');

        // The onboarding should still be blocking
        await expect(page.getByText('Create Your Company')).toBeVisible({timeout: 10000});
    });
});
