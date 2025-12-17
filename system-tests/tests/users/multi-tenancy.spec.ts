import {expect, test} from '@playwright/test';
import {
    createCompany,
    expectOnboardingVisible,
    getDisplayedUserEmails,
    loginUser,
    navigateToUsersTab,
    registerUser,
    uniqueEmail
} from '../../utils';

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
        await expectOnboardingVisible(pageNoCompany);

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
        await expectOnboardingVisible(page);

        // Try to click Users tab (if visible behind overlay)
        // The onboarding overlay should prevent access to the users tab
        // Or if the user somehow gets to users, the API should return an error

        // Close the overlay by clicking outside (if possible) or navigating directly
        await page.goto('/dashboard');

        // The onboarding should still be blocking
        await expectOnboardingVisible(page);
    });
});
